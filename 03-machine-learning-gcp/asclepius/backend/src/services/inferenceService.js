const tf = require('@tensorflow/tfjs');
const InputError = require('../exceptions/InputError');
const jpeg = require('jpeg-js');
const { PNG } = require('pngjs');

async function predictClassification(model, imageBuffer) {
  try {
    let tensor;
    try {
      tensor = parseImageToTensor(imageBuffer);
    } catch (err) {
      throw new InputError('Terjadi kesalahan dalam melakukan prediksi');
    }

    const prediction = model.predict(tensor);
    const score = await prediction.data();
    const confidenceScore = score[0];

    tensor.dispose();
    prediction.dispose();

    const isCancer = confidenceScore > 0.5;
    const result = isCancer ? 'Cancer' : 'Non-cancer';
    const suggestion = isCancer
      ? 'Segera periksa ke dokter!'
      : 'Penyakit kanker tidak terdeteksi.';

    return { confidenceScore, result, suggestion };
  } catch (error) {
    if (error instanceof InputError) {
      throw error;
    }
    throw new InputError('Terjadi kesalahan dalam melakukan prediksi');
  }
}

// Helper to decode JPEG / PNG buffer into 224x224 RGB Float32 Tensor
function parseImageToTensor(buffer) {
  if (!buffer || buffer.length < 8) {
    throw new InputError('Terjadi kesalahan dalam melakukan prediksi');
  }
  
  const isJpeg = buffer[0] === 0xFF && buffer[1] === 0xD8 && buffer[2] === 0xFF;
  const isPng = buffer[0] === 0x89 && buffer[1] === 0x50 && buffer[2] === 0x4E && buffer[3] === 0x47;

  if (!isJpeg && !isPng) {
    throw new InputError('Terjadi kesalahan dalam melakukan prediksi');
  }

  let width, height, data;
  if (isJpeg) {
    // Check if grayscale or corrupted
    // In JPEG format, SOF (Start of Frame) defines number of color components (SOF0: 0xFF, 0xC0 or SOF2: 0xFF, 0xC2)
    const components = getJpegColorComponents(buffer);
    if (components !== 3) {
      // Model expects 3 color channels (RGB), reject 1 channel grayscale as bad request
      throw new InputError('Terjadi kesalahan dalam melakukan prediksi');
    }

    const raw = jpeg.decode(buffer, { useTArray: true, formatAsRGBA: false });
    width = raw.width;
    height = raw.height;
    data = raw.data;
  } else {
    const png = PNG.sync.read(buffer);
    width = png.width;
    height = png.height;
    // png.data is RGBA (4 bytes per pixel)
    const rgbData = new Uint8Array(width * height * 3);
    for (let i = 0, j = 0; i < png.data.length; i += 4, j += 3) {
      rgbData[j] = png.data[i];
      rgbData[j + 1] = png.data[i + 1];
      rgbData[j + 2] = png.data[i + 2];
    }
    data = rgbData;
  }

  const tensor3d = tf.tensor3d(data, [height, width, 3], 'int32');
  const resized = tf.image.resizeBilinear(tensor3d, [224, 224]);
  const expanded = resized.expandDims(0);
  
  tensor3d.dispose();
  return expanded;
}

function getJpegColorComponents(buffer) {
  let offset = 2;
  while (offset < buffer.length) {
    if (buffer[offset] !== 0xFF) {
      break;
    }
    const marker = buffer[offset + 1];
    // SOF0 (0xC0), SOF1 (0xC1), SOF2 (0xC2)
    if (marker === 0xC0 || marker === 0xC1 || marker === 0xC2) {
      // Component count is at offset + 9
      return buffer[offset + 9];
    }
    // Next marker length is stored in 2 bytes
    const length = (buffer[offset + 2] << 8) + buffer[offset + 3];
    offset += 2 + length;
  }
  return 3;
}

module.exports = predictClassification;
