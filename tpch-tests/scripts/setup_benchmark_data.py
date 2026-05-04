#!/usr/bin/env python3

from __future__ import annotations

import argparse
import csv
import hashlib
import shutil
import subprocess
import tempfile
from dataclasses import dataclass
from decimal import Decimal
from pathlib import Path


@dataclass(frozen=True)
class TableSpec:
    columns: list[tuple[str, str]]
    primary_key: list[str]


TABLE_SPECS: dict[str, TableSpec] = {
    "customer": TableSpec(
        columns=[
            ("c_custkey", "BIGINT"),
            ("c_name", "VARCHAR"),
            ("c_address", "VARCHAR"),
            ("c_nationkey", "INTEGER"),
            ("c_phone", "VARCHAR"),
            ("c_acctbal", "DECIMAL(15,2)"),
            ("c_mktsegment", "VARCHAR"),
            ("c_comment", "VARCHAR"),
        ],
        primary_key=["c_custkey"],
    ),
    "lineitem": TableSpec(
        columns=[
            ("l_orderkey", "BIGINT"),
            ("l_partkey", "BIGINT"),
            ("l_suppkey", "BIGINT"),
            ("l_linenumber", "BIGINT"),
            ("l_quantity", "DECIMAL(15,2)"),
            ("l_extendedprice", "DECIMAL(15,2)"),
            ("l_discount", "DECIMAL(15,2)"),
            ("l_tax", "DECIMAL(15,2)"),
            ("l_returnflag", "VARCHAR"),
            ("l_linestatus", "VARCHAR"),
            ("l_shipdate", "DATE"),
            ("l_commitdate", "DATE"),
            ("l_receiptdate", "DATE"),
            ("l_shipinstruct", "VARCHAR"),
            ("l_shipmode", "VARCHAR"),
            ("l_comment", "VARCHAR"),
        ],
        primary_key=["l_orderkey", "l_linenumber"],
    ),
    "nation": TableSpec(
        columns=[
            ("n_nationkey", "INTEGER"),
            ("n_name", "VARCHAR"),
            ("n_regionkey", "INTEGER"),
            ("n_comment", "VARCHAR"),
        ],
        primary_key=["n_nationkey"],
    ),
    "orders": TableSpec(
        columns=[
            ("o_orderkey", "BIGINT"),
            ("o_custkey", "BIGINT"),
            ("o_orderstatus", "VARCHAR"),
            ("o_totalprice", "DECIMAL(15,2)"),
            ("o_orderdate", "DATE"),
            ("o_orderpriority", "VARCHAR"),
            ("o_clerk", "VARCHAR"),
            ("o_shippriority", "INTEGER"),
            ("o_comment", "VARCHAR"),
        ],
        primary_key=["o_orderkey"],
    ),
    "part": TableSpec(
        columns=[
            ("p_partkey", "BIGINT"),
            ("p_name", "VARCHAR"),
            ("p_mfgr", "VARCHAR"),
            ("p_brand", "VARCHAR"),
            ("p_type", "VARCHAR"),
            ("p_size", "INTEGER"),
            ("p_container", "VARCHAR"),
            ("p_retailprice", "DECIMAL(15,2)"),
            ("p_comment", "VARCHAR"),
        ],
        primary_key=["p_partkey"],
    ),
    "partsupp": TableSpec(
        columns=[
            ("ps_partkey", "BIGINT"),
            ("ps_suppkey", "BIGINT"),
            ("ps_availqty", "BIGINT"),
            ("ps_supplycost", "DECIMAL(15,2)"),
            ("ps_comment", "VARCHAR"),
        ],
        primary_key=["ps_partkey", "ps_suppkey"],
    ),
    "region": TableSpec(
        columns=[
            ("r_regionkey", "INTEGER"),
            ("r_name", "VARCHAR"),
            ("r_comment", "VARCHAR"),
        ],
        primary_key=["r_regionkey"],
    ),
    "supplier": TableSpec(
        columns=[
            ("s_suppkey", "BIGINT"),
            ("s_name", "VARCHAR"),
            ("s_address", "VARCHAR"),
            ("s_nationkey", "INTEGER"),
            ("s_phone", "VARCHAR"),
            ("s_acctbal", "DECIMAL(15,2)"),
            ("s_comment", "VARCHAR"),
        ],
        primary_key=["s_suppkey"],
    ),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate committed TPCH CSV fixtures for tpch-tests."
    )
    parser.add_argument(
        "--scale-factor",
        default="1",
        help="TPCH scale factor folder to generate. Default: 1",
    )
    parser.add_argument(
        "--output-root",
        default=None,
        help="Override output root. Default: tpch-tests/src/test/resources/data",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Replace an existing fixture directory.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    repo_root = Path(__file__).resolve().parents[2]
    output_root = (
        Path(args.output_root).resolve()
        if args.output_root
        else repo_root / "tpch-tests" / "src" / "test" / "resources" / "data"
    )
    output_dir = output_root / args.scale_factor
    tmp_dir = Path(tempfile.mkdtemp(prefix="tpch-fixture-"))

    try:
        if output_dir.exists():
            if not args.force:
                raise SystemExit(
                    f"{output_dir} already exists; rerun with --force to replace it."
                )
            shutil.rmtree(output_dir)

        generator_cmd = resolve_generator_command()
        run_generator(generator_cmd, args.scale_factor, tmp_dir)

        output_dir.mkdir(parents=True, exist_ok=True)
        row_counts = convert_tbl_files(tmp_dir, output_dir)
        write_schema(output_dir / "schema.sql")
        write_manifest(output_dir / "manifest.properties", args.scale_factor, row_counts)
        write_checksums(output_dir)
    finally:
        shutil.rmtree(tmp_dir, ignore_errors=True)


def resolve_generator_command() -> list[str]:
    if shutil.which("tpchgen-cli"):
        return ["tpchgen-cli"]
    if shutil.which("uvx"):
        return ["uvx", "--from", "tpchgen-cli", "tpchgen-cli"]
    raise SystemExit("tpchgen-cli or uvx is required to generate TPCH fixtures.")


def run_generator(generator_cmd: list[str], scale_factor: str, output_dir: Path) -> None:
    subprocess.run(
        [
            *generator_cmd,
            "--scale-factor",
            scale_factor,
            "--output-dir",
            str(output_dir),
        ],
        check=True,
    )


def convert_tbl_files(source_dir: Path, output_dir: Path) -> dict[str, int]:
    row_counts: dict[str, int] = {}
    for table_name, spec in TABLE_SPECS.items():
        input_path = source_dir / f"{table_name}.tbl"
        output_path = output_dir / f"{table_name}.csv"
        if not input_path.exists():
            raise SystemExit(f"Missing generator output: {input_path}")

        expected_headers = [name for name, _ in spec.columns]
        row_count = 0
        with input_path.open("r", newline="", encoding="utf-8") as raw_in:
            reader = csv.reader(raw_in, delimiter="|")
            with output_path.open("w", newline="", encoding="utf-8") as raw_out:
                writer = csv.writer(raw_out)
                writer.writerow(expected_headers)
                for row in reader:
                    if not row:
                        continue
                    if row[-1] == "":
                        row = row[:-1]
                    if not row:
                        continue
                    if len(row) != len(expected_headers):
                        raise SystemExit(
                            f"{table_name}: expected {len(expected_headers)} columns, got {len(row)}"
                        )
                    writer.writerow(row)
                    row_count += 1

        if row_count == 0:
            raise SystemExit(f"{table_name}: generated fixture is empty")
        row_counts[table_name] = row_count

    return row_counts


def write_schema(output_path: Path) -> None:
    statements = []
    for table_name, spec in TABLE_SPECS.items():
        columns = ",\n    ".join(f"{name} {sql_type}" for name, sql_type in spec.columns)
        statements.append(f"CREATE TABLE {table_name} (\n    {columns}\n);")
    output_path.write_text("\n\n".join(statements) + "\n", encoding="utf-8")


def write_manifest(output_path: Path, scale_factor: str, row_counts: dict[str, int]) -> None:
    lines = [
        "# Generated by tpch-tests/scripts/setup_benchmark_data.py",
        f"scaleFactor={normalize_scale_factor(scale_factor)}",
        f"tables={','.join(TABLE_SPECS)}",
    ]
    for table_name, spec in TABLE_SPECS.items():
        headers = ",".join(name for name, _ in spec.columns)
        lines.append(f"table.{table_name}.columns={headers}")
        lines.append(f"table.{table_name}.rowCount={row_counts[table_name]}")
        lines.append(f"table.{table_name}.pk={','.join(spec.primary_key)}")
    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_checksums(output_dir: Path) -> None:
    checksum_lines = []
    for path in sorted(output_dir.glob("*")):
        if path.name == "checksums.sha256":
            continue
        checksum = hashlib.sha256(path.read_bytes()).hexdigest()
        checksum_lines.append(f"{checksum}  {path.name}")
    (output_dir / "checksums.sha256").write_text(
        "\n".join(checksum_lines) + "\n",
        encoding="utf-8",
    )


def normalize_scale_factor(value: str) -> str:
    return format(Decimal(value).normalize(), "f")


if __name__ == "__main__":
    main()
