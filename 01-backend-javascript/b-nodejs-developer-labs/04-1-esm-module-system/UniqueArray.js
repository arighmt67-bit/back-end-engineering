class UniqueArray extends Array {
  constructor(...values) {
    const newValues = [...new Set(values)];
    super(...newValues);
  }

  push(...values) {
    const newValues = [...new Set(values)];
    super.push(...newValues);
  }
}

export default UniqueArray;
