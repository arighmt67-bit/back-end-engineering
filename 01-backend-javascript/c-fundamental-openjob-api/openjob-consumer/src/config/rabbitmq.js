/* Konfigurasi RabbitMQ. Kredensial diambil dari environment variables. */
const buatUrl = () => {
  if (process.env.AMQP_URL) return process.env.AMQP_URL;

  const host = process.env.RABBITMQ_HOST || 'localhost';
  const port = process.env.RABBITMQ_PORT || 5672;
  const user = process.env.RABBITMQ_USER || 'guest';
  const password = process.env.RABBITMQ_PASSWORD || 'guest';

  return `amqp://${encodeURIComponent(user)}:${encodeURIComponent(password)}@${host}:${port}`;
};

/* Nama antrean dipakai bersama oleh producer dan consumer. */
const QUEUE = 'application:new';

module.exports = { buatUrl, QUEUE };
