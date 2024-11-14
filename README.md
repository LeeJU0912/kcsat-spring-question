# 스프링 AI 문제 생성 서버

## 사용 기술 스택
1. Spring Boot 3.3.4
2. Kafka

## 기술 이용
1. Kafka의 Offset 메타데이터를 활용하여 실시간 대기열 확인 기능을 구현.
2. 생성에 여러 개의 Topic을 사용하여 GPU 병렬 처리 진행 (파티션 단위로 하려고 했으나, DefaultPartitioner인 Sticky에서 RoundRobin으로 알고리즘을 바꾸어도 완전한 RoundRobin 처리를 하지 않는 문제점이 존재하였음.)
3. Spring Kafka Consumer는 Map\<UUID, Queue\<Message\>\> 형태로 만들어진 문제들에 대해 Session UUID로 분류하여 저장. Session이 삭제될 때, Map UUID의 모든 자료를 삭제하는 것으로 메모리 누수 방지.
4. 사용자가 결과를 받지 않고 여러 번 생성 요청을 하면, Queue의 나머지 메시지를 다 버리고 가장 마지막 요청 하나만을 수용함.

## 핵심 구현 기능
1. AI 문제, 해설 생성
   - 문제, 해설 생성에 각각 2개의 GPU 서버가 구동 중인 상황.
   - 최초에 1회 접속하는 것으로 새로운 Session UUID 생성.
   - 2개의 서버에 번갈아가며 AI 생성 요청 시도 후 현재 자신의 Offset 위치 반환.
   - (현재 자신의 Offset - 최근 Consume된 Offset) 계산 API로 자신의 실시간 위치 확인.
   - 생성된 UUID를 기준으로 Queue에 데이터를 저장하고 가져옴.

---

![Kafka_Diagram.png](Kafka_Diagram.png)