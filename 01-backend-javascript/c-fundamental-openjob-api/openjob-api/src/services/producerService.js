/* Producer RabbitMQ: mengirim pesan saat kandidat membuat lamaran.

   Koneksi dibuat sekali lalu digunakan ulang. Pengiriman sengaja TIDAK ditunggu
   oleh handler HTTP sehingga respons API tetap cepat (asynchronous). */
const amqp = require('amqplib');
const { buatUrl, QUEUE } = require('../config/rabbitmq');

let koneksi = null;
let channel = null;

const dapatkanChannel = async () => {
  if (channel) return channel;

  koneksi = await amqp.connect(buatUrl());
  channel = await koneksi.createChannel();
  await channel.assertQueue(QUEUE, { durable: true });

  /* Bila koneksi terputus, siapkan pembuatan ulang pada pengiriman berikutnya. */
  const reset = () => { channel = null; koneksi = null; };
  koneksi.on('close', reset);
  koneksi.on('error', reset);

  return channel;
};

const ProducerService = {
  /* Payload sengaja hanya berisi application_id sesuai ketentuan kriteria. */
  async kirimLamaranBaru(applicationId) {
    try {
      const ch = await dapatkanChannel();
      ch.sendToQueue(
        QUEUE,
        Buffer.from(JSON.stringify({ application_id: applicationId })),
        { persistent: true },
      );
      console.log(`[producer] pesan dikirim untuk ${applicationId}`);
    } catch (e) {
      /* Kegagalan message broker tidak boleh menggagalkan pembuatan lamaran. */
      console.error('[producer] gagal mengirim pesan:', e.message);
    }
  },

  async tutup() {
    try {
      if (channel) await channel.close();
      if (koneksi) await koneksi.close();
    } catch (e) { /* diabaikan saat proses berhenti */ }
    channel = null;
    koneksi = null;
  },
};

module.exports = ProducerService;
