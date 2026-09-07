/* Consumer RabbitMQ: memproses lamaran baru secara asynchronous, lalu mengirim
   email notifikasi kepada PEMILIK lowongan (bukan pelamar) menggunakan Nodemailer.

   Jalankan dengan: npm run consumer */
require('dotenv').config();
const amqp = require('amqplib');
const pool = require('./config/database');
const { buatUrl, QUEUE } = require('./config/rabbitmq');
const MailSender = require('./services/mailSender');

/* Seluruh data email diambil dari database, tidak ada yang hardcoded. */
const ambilDetailLamaran = async (applicationId) => {
  const result = await pool.query(
    `SELECT
       applications.id                AS application_id,
       applications.created_at        AS tanggal_lamaran,
       pelamar.name                   AS nama_pelamar,
       pelamar.email                  AS email_pelamar,
       jobs.title                     AS judul_pekerjaan,
       companies.name                 AS nama_perusahaan,
       pemilik.name                   AS nama_pemilik,
       pemilik.email                  AS email_pemilik
     FROM applications
     JOIN users     AS pelamar   ON applications.user_id  = pelamar.id
     JOIN jobs                   ON applications.job_id   = jobs.id
     JOIN companies              ON jobs.company_id       = companies.id
     LEFT JOIN users AS pemilik  ON companies.owner_id    = pemilik.id
     WHERE applications.id = $1`,
    [applicationId],
  );

  return result.rowCount === 0 ? null : result.rows[0];
};

const prosesPesan = async (channel, msg) => {
  if (!msg) return;

  try {
    const { application_id: applicationId } = JSON.parse(msg.content.toString());
    console.log(`[consumer] menerima lamaran ${applicationId}`);

    const detail = await ambilDetailLamaran(applicationId);

    if (!detail) {
      console.warn(`[consumer] lamaran ${applicationId} tidak ditemukan, pesan dibuang`);
      return channel.ack(msg);
    }

    /* Hanya pemilik lowongan yang menerima notifikasi. */
    if (!detail.email_pemilik) {
      console.warn(`[consumer] lowongan ${detail.judul_pekerjaan} belum memiliki pemilik, email dilewati`);
      return channel.ack(msg);
    }

    await MailSender.kirimNotifikasiLamaran(detail.email_pemilik, detail);
    console.log(`[consumer] email terkirim ke pemilik lowongan: ${detail.email_pemilik}`);

    return channel.ack(msg);
  } catch (e) {
    console.error('[consumer] gagal memproses pesan:', e.message);
    /* Pesan tidak dikembalikan ke antrean agar tidak berputar tanpa henti. */
    return channel.ack(msg);
  }
};

const jalankan = async () => {
  const koneksi = await amqp.connect(buatUrl());
  const channel = await koneksi.createChannel();

  await channel.assertQueue(QUEUE, { durable: true });
  channel.prefetch(1);

  console.log(`[consumer] menunggu pesan pada antrean "${QUEUE}"`);
  await channel.consume(QUEUE, (msg) => prosesPesan(channel, msg));
};

jalankan().catch((e) => {
  console.error('[consumer] gagal dijalankan:', e.message);
  process.exit(1);
});
