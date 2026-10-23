# syntax=docker/dockerfile:1

########## build stage ##########
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 1) 빌드 설정만 먼저 복사해 의존성 레이어를 캐시한다.
COPY gradlew ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
# Windows에서 커밋하면 실행 권한이 빠질 수 있으므로 명시적으로 부여한다.
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

# 2) 소스를 복사해 실행 jar를 만든다. (테스트는 CI/로컬 build에서 수행)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 3) 레이어를 분리한다.
RUN java -Djarmode=tools -jar build/libs/app.jar extract --layers --launcher --destination extracted

########## runtime stage ##########
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

# 변경 빈도가 낮은 레이어부터 복사한다.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
