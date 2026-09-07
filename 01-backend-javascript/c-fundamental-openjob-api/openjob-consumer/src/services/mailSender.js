/* Pengirim email berbasis Nodemailer. Seluruh kredensial dibaca dari environment variables. */
const nodemailer = require('nodemailer');

const transporter = nodemailer.createTransport({
  host: process.env.MAIL_HOST,
  port: Number(process.env.MAIL_PORT),
  secure: Number(process.env.MAIL_PORT) === 465,
  auth: {
    user: process.env.MAIL_USER,
    pass: process.env.MAIL_PASSWORD,
  },
});

const formatTanggal = (nilai) => new Date(nilai).toLocaleString('id-ID', {
  dateStyle: 'full',
  timeStyle: 'short',
  timeZone: 'Asia/Jakarta',
});

const MailSender = {
  /* Email memuat data pelamar dan tanggal lamaran, seluruhnya berasal dari database. */
  async kirimNotifikasiLamaran(tujuan, detail) {
    const tanggal = formatTanggal(detail.tanggal_lamaran);

    const html = `
      <h2>Lamaran Baru untuk ${detail.judul_pekerjaan}</h2>
      <p>Halo ${detail.nama_pemilik},</p>
      <p>Seorang kandidat baru saja melamar lowongan di ${detail.nama_perusahaan}.</p>
      <table cellpadding="6" border="1" style="border-collapse:collapse">
        <tr><td><b>Nama pelamar</b></td><td>${detail.nama_pelamar}</td></tr>
        <tr><td><b>Email pelamar</b></td><td>${detail.email_pelamar}</td></tr>
        <tr><td><b>Tanggal lamaran</b></td><td>${tanggal}</td></tr>
        <tr><td><b>Lowongan</b></td><td>${detail.judul_pekerjaan}</td></tr>
      </table>
      <p>Silakan masuk ke OpenJob untuk meninjau lamaran tersebut.</p>
    `;

    const teks = [
      `Lamaran baru untuk ${detail.judul_pekerjaan}`,
      `Nama pelamar    : ${detail.nama_pelamar}`,
      `Email pelamar   : ${detail.email_pelamar}`,
      `Tanggal lamaran : ${tanggal}`,
    ].join('\n');

    return transporter.sendMail({
      from: process.env.MAIL_USER,
      to: tujuan,
      subject: `[OpenJob] Lamaran baru dari ${detail.nama_pelamar} untuk ${detail.judul_pekerjaan}`,
      text: teks,
      html,
    });
  },
};

module.exports = MailSender;
