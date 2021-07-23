#FROM node:16.5.0 as build
#WORKDIR /usr/local/app
#COPY ./ /usr/local/app/
#RUN npm install
#RUN npm run-script ng -- build


FROM nginx:latest
#COPY --from=build /usr/local/app/dist/rocketchat-archive /usr/share/nginx/html
COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/rocketchat-archive /usr/share/nginx/html
EXPOSE 80

