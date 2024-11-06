ARG APP_INSIGHTS_AGENT_VERSION=3.6.1
FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/notifications-service.jar /opt/app/

EXPOSE 8080
CMD [ \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "notifications-service.jar" \
    ]
