-- 채널톡 API Secret 컬럼 추가
-- Integration 테이블에 channel_talk_api_secret 컬럼 추가

ALTER TABLE integration 
ADD COLUMN IF NOT EXISTS channel_talk_api_secret TEXT;

-- 기존 데이터가 있다면 NULL로 유지 (필요시 업데이트 필요)

