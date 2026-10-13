FROM mcr.microsoft.com/dotnet/sdk:8.0-bookworm-slim@sha256:bb32ba3ba3ea36e38572d9d8db76fa15f7cbf722f3f886e06bca6d528bd4fba8

ENV DOTNET_CLI_TELEMETRY_OPTOUT=1 \
    DOTNET_NOLOGO=1

RUN dotnet tool install \
        --tool-path /opt/sqlpackage \
        Microsoft.SqlPackage \
        --version 170.5.76 \
    && /opt/sqlpackage/sqlpackage /Version

USER app

ENTRYPOINT ["/opt/sqlpackage/sqlpackage"]
