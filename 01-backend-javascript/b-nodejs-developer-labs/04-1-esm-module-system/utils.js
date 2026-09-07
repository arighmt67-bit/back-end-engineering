function unword(word) {
  return [...word];
}

function reword(...chars) {
  return chars.reduce((word, char) => word + char, '');
}

export { unword, reword };
