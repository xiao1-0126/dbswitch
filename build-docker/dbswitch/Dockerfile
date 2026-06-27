FROM registry.cn-hangzhou.aliyuncs.com/inrgihc/openjdk:8-jre-alpine

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Required by Spring @Value("${APP_DRIVERS_PATH}")
ENV APP_DRIVERS_PATH=/dbswitch-release/drivers

COPY dbswitch-release /dbswitch-release

EXPOSE      9088

USER        root

WORKDIR /dbswitch-release

RUN chmod u+x /dbswitch-release/bin/startup.sh

CMD  ["/bin/sh", "/dbswitch-release/bin/startup.sh"]

