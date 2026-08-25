# Build context is the repository root (see docker-compose.yml) so this
# stage can reach the frontend/ directory.
FROM node:20 AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
COPY frontend/ ./
RUN npm run build

FROM httpd:2.4

# mod_proxy / mod_proxy_http / mod_rewrite / mod_headers ship with the image
# but are commented out in the default config -- enable them.
RUN sed -i \
    -e 's/^#\(LoadModule proxy_module .*\)/\1/' \
    -e 's/^#\(LoadModule proxy_http_module .*\)/\1/' \
    -e 's/^#\(LoadModule rewrite_module .*\)/\1/' \
    -e 's/^#\(LoadModule headers_module .*\)/\1/' \
    conf/httpd.conf

COPY proxy/httpd-app.conf /usr/local/apache2/conf/extra/httpd-app.conf
RUN echo 'Include conf/extra/httpd-app.conf' >> conf/httpd.conf

COPY --from=frontend-build /app/dist /usr/local/apache2/htdocs/ui

EXPOSE 80
