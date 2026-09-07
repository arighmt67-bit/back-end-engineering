function parseURL(url) {
  try {
    return new URL(url);
  } catch (error) {
    return null;
  }
}
