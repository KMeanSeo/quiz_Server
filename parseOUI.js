const fs = require('fs');

function parseTxtToJson(inputFile, outputFile) {
    try {
        const data = fs.readFileSync(inputFile, 'utf8');
        const lines = data.split('\n'); // 줄 단위로 나누기
        const result = []; // 결과를 저장할 배열

        let currentOUI = null; // 현재 처리 중인 OUI
        let currentData = {}; // 현재 OUI의 데이터를 저장

        lines.forEach((line) => {
            line = line.trim(); // 양쪽 공백 제거

            if (!line) {
                // 빈 줄은 무시
                return;
            }

            const match = line.match(/^([\dA-F]{2}-[\dA-F]{2}-[\dA-F]{2})\s+\((hex|base 16)\)\s+(.*)$/);
            if (match) {
                // 새로운 OUI 항목 발견
                if (currentOUI) {
                    // 이전 OUI 데이터를 저장
                    result.push(currentData);
                }

                // 새로운 OUI 데이터 초기화
                currentOUI = match[1].replace(/-/g, ':').toUpperCase(); // MAC 형식으로 변환
                currentData = {
                    oui: currentOUI,
                    organization: match[3].trim(),
                    base16: null, // base 16 데이터를 저장할 필드
                };
            } else if (currentOUI && line.includes('(base 16)')) {
                // base 16 정보 추출
                const base16Match = line.match(/^([\dA-F]+)\s+\(base 16\)/);
                if (base16Match) {
                    currentData.base16 = base16Match[1];
                }
            }
        });

        // 마지막 OUI 데이터를 저장
        if (currentOUI) {
            result.push(currentData);
        }

        // JSON 파일로 저장
        fs.writeFileSync(outputFile, JSON.stringify(result, null, 2));
        console.log(`Converted OUI data saved to ${outputFile}`);
    } catch (error) {
        console.error(`Error: ${error.message}`);
    }
}

// 실행
const inputFile = 'oui.txt'; // 입력 파일 경로
const outputFile = 'oui.json'; // 출력 파일 경로
parseTxtToJson(inputFile, outputFile);
