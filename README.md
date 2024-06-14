# 2024년도 소프트웨어 설계 및 실험 텀 프로젝트
+ 빌드 및 실행 환경
    + Android Studio Jellyfish
    + Android 10
+ 기능
    + Wi-Fi 네트워크의 비밀번호의 사진을 찍어 연결할 수 있습니다.
+ 구현
  1. 카메라 인텐트로 사진을 찍습니다.
  2. Canhub/Android-Image-Cropper 라이브러리로 비밀번호 부분만 자릅니다.
  3. Onnx 모델을 통해 비밀번호를 이미지에서 추출합니다.
  4. 검색된 SSID 중 올바른 것을 선택하게 한 다음, 추출한 비밀번호를 사용해 네트워크 제안에 해당 AP를 추가합니다.

# Preprocessing 
## Get OCR Model
0. [Click to GitHub repository that includes training code the OCR Model if you want](https://github.com/bluelemon61/WIFI-capture-model)

1. [Download OCR model on Google Drive](https://drive.google.com/file/d/1UhUPa-_4R8eeKn1Z0iWjuMo5YEQdmjB9/view?usp=drive_link)
2. Change the model's name to `model.onnx`
3. Put the model in directory `app/src/main/assets/model.onnx`