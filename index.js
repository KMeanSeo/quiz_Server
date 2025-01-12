const express = require('express');
const multer = require('multer');
const app = express();
const http = require('http').Server(app);
const io = require('socket.io')(http);
const path = require('path');
const { exec } = require('child_process');
const fs = require('fs');

const users = {};
const messages = [];
const connectedUsers = {};
const nasDirectory = '/home/user2/lab-server/nas';
const ouiFilePath = './oui.json'; // OUI JSON 파일 경로

// 로컬 OUI 데이터 로드
let ouiData = {};
try {
    const rawData = JSON.parse(fs.readFileSync(ouiFilePath, 'utf8'));
    ouiData = rawData.reduce((acc, entry) => {
        acc[entry.oui.toLowerCase()] = entry.organization;
        return acc;
    }, {});
    console.log(`Loaded OUI data from ${ouiFilePath}`);
} catch (err) {
    console.error(`Error loading OUI data: ${err.message}`);
}

// MAC 주소로 제조사 조회
function getVendorByMac(mac, callback) {
    const oui = mac.slice(0, 8).toLowerCase();
    const vendor = ouiData[oui] || 'Unknown Vendor';
    callback(vendor);
}

// NetBIOS 이름 조회
function getNetbiosName(ip, callback) {
    exec(`nmblookup -A ${ip}`, (err, stdout) => {
        if (err || !stdout) {
            callback(null);
            return;
        }
        const match = stdout.match(/\s+(.+)\s+<00>/m);
        callback(match ? match[1].trim() : null);
    });
}

// mDNS 이름 조회
function getMdnsName(ip, callback) {
    exec(`avahi-resolve -a ${ip}`, (err, stdout) => {
        if (err || !stdout) {
            callback(null);
            return;
        }
        const match = stdout.match(/\s+(.+)\s+/);
        callback(match ? match[1].trim() : null);
    });
}

// 네트워크 스캔 및 디바이스 탐색
const devicesMap = new Map();

function updateDevices(callback) {
    exec('arp-scan --interface=wlan0 -l', (err, stdout) => {
        if (err) {
            console.error(`Error scanning network: ${err}`);
            if (callback) callback(err);
            return;
        }

        const lines = stdout.split('\n');
        const currentDevices = new Set();
        let pending = 0;

        lines.forEach((line) => {
            const match = line.match(/^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})\s+([\w:]+)\s+(.*)$/);
            if (match) {
                const ip = match[1];
                const mac = match[2].toLowerCase();
                currentDevices.add(mac);

                if (!devicesMap.has(mac)) {
                    devicesMap.set(mac, { ip, mac, vendor: 'Unknown Vendor', name: null, lastSeen: Date.now(), active: true });
                }

                const device = devicesMap.get(mac);
                device.ip = ip;
                device.lastSeen = Date.now();
                device.active = true;

                pending++;

                getNetbiosName(ip, (netbiosName) => {
                    if (netbiosName) device.name = netbiosName;

                    if (!netbiosName) {
                        getMdnsName(ip, (mdnsName) => {
                            if (mdnsName) device.name = mdnsName;

                            getVendorByMac(mac, (vendor) => {
                                device.vendor = vendor;
                                pending--;
                                if (pending === 0 && callback) callback(null);
                            });
                        });
                    } else {
                        pending--;
                        if (pending === 0 && callback) callback(null);
                    }
                });
            }
        });

        devicesMap.forEach((device, mac) => {
            if (!currentDevices.has(mac)) {
                if (Date.now() - device.lastSeen > 30000) {
                    device.active = false;
                }
            }
        });

        if (pending === 0 && callback) callback(null);
    });
}

// 디바이스 목록 브로드캐스트
function broadcastDevices() {
    io.emit('update-devices', Array.from(devicesMap.values()));
}

// 주기적인 네트워크 스캔 실행
setInterval(() => {
    updateDevices((err) => {
        if (!err) {
            broadcastDevices();
        }
    });
}, 10000); // 10초마다 스캔

// 정적 파일 및 템플릿 설정
app.use(express.static('public'));
app.use(express.urlencoded({ extended: true }));
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// / 라우트 - index.ejs 렌더링
app.get('/', (req, res) => {
    res.render('index', { title: 'Welcome to the Lab Server' });
});

// /network 라우트
app.get('/network', (req, res) => {
    updateDevices((err) => {
        if (err) {
            console.error(`Error updating devices: ${err}`);
            return res.status(500).send('Error updating devices.');
        }
        broadcastDevices(); // 업데이트 후 모든 사용자에게 브로드캐스트
        res.render('network', { title: 'Network Monitor' });
    });
});

// Multer 설정
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, nasDirectory); // NAS 디렉터리에 저장
    },
    filename: (req, file, cb) => {
        cb(null, file.originalname); // 원래 파일 이름 사용
    },
});
const upload = multer({ storage });

// NAS 파일 목록 제공
app.get('/nas', (req, res) => {
    fs.readdir(nasDirectory, (err, files) => {
        if (err) {
            console.error(`Error accessing NAS directory: ${err.message}`);
            return res.status(500).send('Unable to access NAS directory.');
        }

        const fileLinks = files.map((file) => {
            const filePath = path.join(nasDirectory, file);
            const stats = fs.statSync(filePath);
            const fileType = stats.isDirectory() ? '📁' : '📄';
            return `<li>${fileType} <a href="/nas/download/${encodeURIComponent(file)}">${file}</a></li>`;
        }).join('');

        res.send(`
            <html>
            <body>
                <h2>NAS Files</h2>
                <ul>${fileLinks}</ul>
                <form action="/nas/upload" method="post" enctype="multipart/form-data" style="margin-top: 20px;">
                    <input type="file" name="file" />
                    <button type="submit">Upload File</button>
                </form>
            </body>
            </html>
        `);
    });
});

// NAS 파일 다운로드
app.get('/nas/download/:fileName', (req, res) => {
    const fileName = req.params.fileName;
    const filePath = path.join(nasDirectory, fileName);

    fs.access(filePath, fs.constants.F_OK, (err) => {
        if (err) {
            return res.status(404).send('File not found.');
        }

        res.download(filePath, fileName, (err) => {
            if (err) {
                console.error(`Error downloading file: ${err.message}`);
                res.status(500).send('Unable to download the file.');
            }
        });
    });
});

// NAS 파일 업로드
app.post('/nas/upload', upload.single('file'), (req, res) => {
    if (!req.file) {
        return res.status(400).send('No file uploaded.');
    }

    console.log(`File uploaded: ${req.file.originalname}`);
    res.redirect('/nas'); // 업로드 후 NAS 페이지로 리디렉션
});

// 소켓 통신
io.on('connection', (socket) => {
    const clientIp = socket.handshake.address;
    const user = users[clientIp];

    if (user) {
        socket.emit('chat history', messages.filter((m) => m.name === user.name));
    }

    socket.on('chat message', (msg) => {
        const user = users[clientIp];
        if (user) {
            const chatMessage = { name: user.name, message: msg, timestamp: Date.now() };
            messages.push(chatMessage);
            io.emit('chat message', chatMessage);
        }
    });

    socket.on('request-update', () => {
        socket.emit('update-devices', Array.from(devicesMap.values()));
    });

    socket.on('disconnect', () => {
        delete connectedUsers[clientIp];
        io.emit('update-users', Object.values(connectedUsers));
    });
});

// 서버 실행
const PORT = 3000;
http.listen(PORT, () => {
    console.log(`Server running at http://192.168.45.127:${PORT}`);
});