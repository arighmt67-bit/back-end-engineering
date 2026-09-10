# unitconv - CLI Unit Converter in Rust

A robust command-line application for converting temperature and length units built with Rust.

## Features (Dicoding Bintang 5 Criteria)
- **Temperature Conversions**: `celsius`, `fahrenheit`, `kelvin`
- **Length Conversions**: `cm`, `inch`, `km`, `miles`
- **Unit Discovery**: `unitconv list` displays all supported units categorized by type.
- **Persistence**: Automatically records conversion transactions to local `conversion.json`.
- **Audit History**: `unitconv history` prints chronologically indexed conversion history.
- **Strict Error Handling**: Gracefully catches unknown units and blocks cross-category conversions (`[panjang] → [suhu]`).

## Building and Running

### Build Release
```bash
cargo build --release
```

### Usage Examples
```bash
# Temperature
./target/release/unitconv convert --from celsius --to fahrenheit --value 100
# Output: 100.0 °C = 212.0 °F

# Length
./target/release/unitconv convert --from cm --to km --value 16000
# Output: 16000.0 cm = 0.16 km

# List Units
./target/release/unitconv list

# History
./target/release/unitconv history
```
