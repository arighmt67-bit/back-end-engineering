use clap::{Args, Parser, Subcommand};
use serde::{Deserialize, Serialize};
use std::fs::{File, OpenOptions};
use std::io::{BufReader, BufWriter};
use std::path::Path;

#[derive(Parser)]
#[command(name = "unitconv")]
#[command(about = "A CLI tool for temperature and length unit conversions", long_about = None)]
struct Cli {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
enum Commands {
    /// Convert between units (temperature or length)
    Convert(ConvertArgs),
    /// List all supported units grouped by category
    List,
    /// Display conversion history from conversion.json
    History,
}

#[derive(Args)]
struct ConvertArgs {
    /// Source unit (e.g. celsius, fahrenheit, kelvin, cm, inch, km, miles)
    #[arg(long)]
    from: String,

    /// Target unit (e.g. celsius, fahrenheit, kelvin, cm, inch, km, miles)
    #[arg(long)]
    to: String,

    /// Value to convert
    #[arg(long)]
    value: f64,
}

#[derive(Debug, PartialEq, Eq, Clone, Copy)]
enum UnitCategory {
    Temperature,
    Length,
}

impl std::fmt::Display for UnitCategory {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            UnitCategory::Temperature => write!(f, "suhu"),
            UnitCategory::Length => write!(f, "panjang"),
        }
    }
}

#[derive(Debug, Clone)]
struct UnitInfo {
    name: &'static str,
    category: UnitCategory,
    symbol: &'static str,
}

const SUPPORTED_UNITS: &[UnitInfo] = &[
    UnitInfo { name: "celsius", category: UnitCategory::Temperature, symbol: "°C" },
    UnitInfo { name: "fahrenheit", category: UnitCategory::Temperature, symbol: "°F" },
    UnitInfo { name: "kelvin", category: UnitCategory::Temperature, symbol: "K" },
    UnitInfo { name: "cm", category: UnitCategory::Length, symbol: "cm" },
    UnitInfo { name: "inch", category: UnitCategory::Length, symbol: "inch" },
    UnitInfo { name: "km", category: UnitCategory::Length, symbol: "km" },
    UnitInfo { name: "miles", category: UnitCategory::Length, symbol: "miles" },
];

#[derive(Serialize, Deserialize, Debug)]
struct HistoryRecord {
    from_value: f64,
    from_unit: String,
    to_value: f64,
    to_unit: String,
    formatted: String,
}

fn find_unit(name: &str) -> Option<&'static UnitInfo> {
    let lower = name.trim().to_lowercase();
    SUPPORTED_UNITS.iter().find(|u| u.name == lower)
}

fn convert_temperature(from: &str, to: &str, value: f64) -> Result<f64, String> {
    // Convert source to Celsius first
    let celsius = match from {
        "celsius" => value,
        "fahrenheit" => (value - 32.0) * 5.0 / 9.0,
        "kelvin" => value - 273.15,
        _ => return Err(format!("Satuan asal '{}' tidak dikenali.", from)),
    };

    // Convert Celsius to target
    let result = match to {
        "celsius" => celsius,
        "fahrenheit" => (celsius * 9.0 / 5.0) + 32.0,
        "kelvin" => celsius + 273.15,
        _ => return Err(format!("Satuan tujuan '{}' tidak dikenali.", to)),
    };

    Ok(result)
}

fn convert_length(from: &str, to: &str, value: f64) -> Result<f64, String> {
    // Convert source to meters first
    let meters = match from {
        "cm" => value / 100.0,
        "inch" => value * 0.0254,
        "km" => value * 1000.0,
        "miles" => value * 1609.344,
        _ => return Err(format!("Satuan asal '{}' tidak dikenali.", from)),
    };

    // Convert meters to target
    let result = match to {
        "cm" => meters * 100.0,
        "inch" => meters / 0.0254,
        "km" => meters / 1000.0,
        "miles" => meters / 1609.344,
        _ => return Err(format!("Satuan tujuan '{}' tidak dikenali.", to)),
    };

    Ok(result)
}

const HISTORY_FILE: &str = "conversion.json";

fn save_history(record: HistoryRecord) {
    let path = Path::new(HISTORY_FILE);
    let mut history: Vec<HistoryRecord> = if path.exists() {
        match File::open(path) {
            Ok(file) => {
                let reader = BufReader::new(file);
                serde_json::from_reader(reader).unwrap_or_else(|_| Vec::new())
            }
            Err(_) => Vec::new(),
        }
    } else {
        Vec::new()
    };

    history.push(record);

    if let Ok(file) = OpenOptions::new()
        .write(true)
        .create(true)
        .truncate(true)
        .open(path)
    {
        let writer = BufWriter::new(file);
        let _ = serde_json::to_writer_pretty(writer, &history);
    }
}

fn display_history() {
    let path = Path::new(HISTORY_FILE);
    if !path.exists() {
        println!("Riwayat Konversi kosong.");
        return;
    }

    match File::open(path) {
        Ok(file) => {
            let reader = BufReader::new(file);
            let history: Vec<HistoryRecord> = match serde_json::from_reader(reader) {
                Ok(h) => h,
                Err(_) => {
                    println!("Gagal membaca riwayat konversi.");
                    return;
                }
            };

            if history.is_empty() {
                println!("Riwayat Konversi kosong.");
                return;
            }

            println!("Riyawat Konversi:");
            for (idx, item) in history.iter().enumerate() {
                println!("{}. {}", idx + 1, item.formatted);
            }
        }
        Err(_) => {
            println!("Tidak dapat membuka berkas conversion.json.");
        }
    }
}

fn format_number(val: f64) -> String {
    if (val.fract()).abs() < 1e-9 {
        format!("{:.1}", val)
    } else {
        // Trim redundant trailing zeros for floating points
        let s = format!("{:.6}", val);
        let trimmed = s.trim_end_matches('0');
        if trimmed.ends_with('.') {
            format!("{}0", trimmed)
        } else {
            trimmed.to_string()
        }
    }
}

fn main() {
    let cli = Cli::parse();

    match cli.command {
        Commands::List => {
            println!("Satuan yang didukung:");
            for (idx, u) in SUPPORTED_UNITS.iter().enumerate() {
                println!("{}. [{}] {}", idx + 1, u.category, u.name);
            }
        }
        Commands::History => {
            display_history();
        }
        Commands::Convert(args) => {
            let from_lower = args.from.trim().to_lowercase();
            let to_lower = args.to.trim().to_lowercase();

            let from_unit = match find_unit(&from_lower) {
                Some(u) => u,
                None => {
                    eprintln!("Error: [ERROR] Satuan asal '{}' tidak dikenali.", args.from);
                    std::process::exit(1);
                }
            };

            let to_unit = match find_unit(&to_lower) {
                Some(u) => u,
                None => {
                    eprintln!("Error: [ERROR] Satuan tujuan '{}' tidak dikenali.", args.to);
                    std::process::exit(1);
                }
            };

            // Validate cross-category conversion
            if from_unit.category != to_unit.category {
                eprintln!(
                    "Error: [ERROR] Tidak dapat mengonversi satuan yang berbeda kategori: [{}] {} → [{}] {}",
                    from_unit.category, from_unit.name, to_unit.category, to_unit.name
                );
                std::process::exit(1);
            }

            let result = match from_unit.category {
                UnitCategory::Temperature => convert_temperature(&from_lower, &to_lower, args.value),
                UnitCategory::Length => convert_length(&from_lower, &to_lower, args.value),
            };

            match result {
                Ok(converted_val) => {
                    let from_fmt = format_number(args.value);
                    let to_fmt = format_number(converted_val);

                    let output_line = if from_unit.category == UnitCategory::Temperature {
                        format!("{} {} = {} {}", from_fmt, from_unit.symbol, to_fmt, to_unit.symbol)
                    } else {
                        format!("{} {} = {} {}", from_fmt, from_unit.name, to_fmt, to_unit.name)
                    };

                    println!("{}", output_line);

                    // Save to history (conversion.json)
                    save_history(HistoryRecord {
                        from_value: args.value,
                        from_unit: from_unit.name.to_string(),
                        to_value: converted_val,
                        to_unit: to_unit.name.to_string(),
                        formatted: output_line,
                    });
                }
                Err(err_msg) => {
                    eprintln!("Error: [ERROR] {}", err_msg);
                    std::process::exit(1);
                }
            }
        }
    }
}
