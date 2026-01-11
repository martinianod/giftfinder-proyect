# ===== FRONTEND BUILD =====
FROM node:20 AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install

COPY . .
RUN npm run build

# ===== NGINX SERVE =====
FROM nginx:alpine

# Copia el build
COPY --from=build /app/dist /usr/share/nginx/html

# Configurar nginx para SPA + proxy al backend
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
