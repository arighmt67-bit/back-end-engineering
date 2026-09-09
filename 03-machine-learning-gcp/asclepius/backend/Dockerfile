FROM node:18-slim

WORKDIR /app

COPY package*.json ./

RUN npm ci --omit=dev

COPY src ./src

ENV PORT=8080
ENV HOST=0.0.0.0
ENV MODEL_URL=https://storage.googleapis.com/submissionmlgc-arirahmatr-model/model.json
ENV PROJECT_ID=submissionmlgc-arirahmatr
ENV FIRESTORE_DATABASE_ID=(default)

EXPOSE 8080

CMD ["node", "src/server/server.js"]
