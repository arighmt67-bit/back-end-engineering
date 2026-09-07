/* istanbul ignore file */
/**
 * Setup environment untuk pengujian.
 *
 * Berkas ini dimuat Vitest sebelum berkas uji dijalankan. Tujuannya memastikan
 * pengujian SELALU memakai basis data pengujian (.test.env), bukan basis data
 * pengembangan (.env).
 *
 * `override: true` diperlukan karena dotenv secara bawaan tidak menimpa variable
 * yang sudah lebih dulu termuat di process.env.
 */
import dotenv from 'dotenv';
import path from 'path';

process.env.NODE_ENV = 'test';

dotenv.config({
  path: path.resolve(process.cwd(), '.test.env'),
  override: true,
});
