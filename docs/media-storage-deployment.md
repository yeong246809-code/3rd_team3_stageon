# S3 미디어 저장소 배포 체크리스트

## 요청 흐름

`브라우저 -> Cloudflare(stageon.click/media/*) -> EC2 애플리케이션 -> S3 비공개 버킷`

- 새 배너: `image/banners/<uuid>.<확장자>`
- 새 공연 포스터: `image/posters/<uuid>.<확장자>`
- DB URL: `/media/<S3 object key>`
- DB key: 배너 `image_key`, 공연 `poster_key`
- 기존 `/uploads/...` URL은 자동 변경하지 않습니다.

## EC2 애플리케이션 환경 변수

```properties
STORAGE_TYPE=s3
S3_BUCKET=stageon-470914318991-us-east-1-an
S3_REGION=us-east-1
S3_BANNER_PREFIX=image/banners
S3_POSTER_PREFIX=image/posters
S3_FILE_PREFIX=file
MEDIA_URL_PREFIX=/media
```

AWS access key와 secret key는 넣지 않습니다. EC2의 `StageOnEc2S3Role`에서 자격 증명을 자동으로 받습니다.

## 배포 순서

1. 운영 DB에서 `database/migrate-media-storage-to-s3.sql`을 한 번 실행합니다.
2. 위 환경 변수를 EC2 애플리케이션 실행 환경에 추가합니다.
3. 새 애플리케이션을 빌드하고 재시작합니다.
4. 관리자에서 테스트 배너 이미지를 업로드합니다.
5. S3의 `image/banners/`에 객체가 생겼는지 확인합니다.
6. DB `banners.image_url`이 `/media/image/banners/...`, `image_key`가 `image/banners/...`인지 확인합니다.
7. `https://stageon.click/media/image/banners/...`가 200과 이미지 Content-Type을 반환하는지 확인합니다.
8. 같은 URL을 다시 요청해 Cloudflare 응답 헤더의 `CF-Cache-Status`를 확인합니다.

## 기존 404 이미지

기존 `/uploads/banners/...` 및 `/uploads/posters/...` 레코드는 S3 객체가 아니므로 자동 복구되지 않습니다. 원본 파일이 있으면 관리자 수정 화면에서 다시 업로드하고, 원본이 없으면 이미지를 새로 지정해야 합니다.
