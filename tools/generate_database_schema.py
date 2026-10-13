"""Generate the actual DinoGo schema snapshot from a SQL Server bacpac."""

from __future__ import annotations

import argparse
import zipfile
import xml.etree.ElementTree as ET
from datetime import date
from pathlib import Path


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def elements(root: ET.Element, element_type: str | None = None) -> list[ET.Element]:
    result = [node for node in root.iter() if local_name(node.tag) == "Element"]
    return [node for node in result if node.get("Type") == element_type] if element_type else result


def property_value(node: ET.Element, name: str) -> str:
    for child in node:
        if local_name(child.tag) == "Property" and child.get("Name") == name:
            if child.get("Value") is not None:
                return child.get("Value", "")
            for value in child:
                if local_name(value.tag) == "Value":
                    return (value.text or "").strip()
    return ""


def relationship_refs(node: ET.Element, name: str) -> list[str]:
    refs: list[str] = []
    for relation in node:
        if local_name(relation.tag) != "Relationship" or relation.get("Name") != name:
            continue
        for entry in relation:
            for reference in entry:
                if local_name(reference.tag) == "References":
                    refs.append(reference.get("Name", ""))
    return refs


def child_elements(node: ET.Element, relationship: str) -> list[ET.Element]:
    result: list[ET.Element] = []
    for relation in node:
        if local_name(relation.tag) != "Relationship" or relation.get("Name") != relationship:
            continue
        for entry in relation:
            for child in entry:
                if local_name(child.tag) == "Element":
                    result.append(child)
    return result


def table_name(reference: str) -> str:
    parts = reference.replace("][", ".").replace("[", "").replace("]", "").split(".")
    return ".".join(parts[:2])


def column_name(reference: str) -> str:
    return reference.replace("][", ".").replace("[", "").replace("]", "").split(".")[-1]


def object_name(reference: str) -> str:
    return reference.replace("][", ".").replace("[", "").replace("]", "").split(".")[-1]


def markdown(value: str) -> str:
    return value.replace("|", "\\|").replace("\r", " ").replace("\n", " ")


def type_text(column: ET.Element) -> str:
    if column.get("Type") == "SqlComputedColumn":
        return "computed"
    specifiers = child_elements(column, "TypeSpecifier")
    if not specifiers:
        return "unknown"
    specifier = specifiers[0]
    type_refs = relationship_refs(specifier, "Type")
    base = object_name(type_refs[0]) if type_refs else "unknown"
    length, precision, scale = (property_value(specifier, key) for key in ("Length", "Precision", "Scale"))
    if length:
        return f"{base}({'max' if length == '-1' else length})"
    if precision:
        return f"{base}({precision}{',' + scale if scale else ''})"
    if scale and base in {"datetime2", "datetimeoffset", "time"}:
        return f"{base}({scale})"
    return base


def constraint_columns(node: ET.Element) -> list[str]:
    specs = child_elements(node, "ColumnSpecifications") or child_elements(node, "Columns")
    result: list[str] = []
    for spec in specs:
        refs = relationship_refs(spec, "Column")
        if refs:
            result.append(column_name(refs[0]))
    return result


def add_list(lines: list[str], title: str, items: list[str]) -> None:
    lines.extend((f"### {title}", *(items or ["無。"]), ""))


def generate(bacpac: Path, output: Path) -> None:
    with zipfile.ZipFile(bacpac) as archive:
        root = ET.fromstring(archive.read("model.xml"))
        metadata = ET.fromstring(archive.read("DacMetadata.xml"))

    database = next((child.text for child in metadata if local_name(child.tag) == "Name"), "DinoGo")
    all_elements = elements(root)
    schemas = sorted(object_name(node.get("Name", "")) for node in all_elements if node.get("Type") == "SqlSchema")
    tables = sorted((node for node in all_elements if node.get("Type") == "SqlTable"), key=lambda node: table_name(node.get("Name", "")))
    constraint_types = {"SqlPrimaryKeyConstraint", "SqlForeignKeyConstraint", "SqlUniqueConstraint", "SqlCheckConstraint", "SqlDefaultConstraint", "SqlIndex"}
    constraints = [node for node in all_elements if node.get("Type") in constraint_types]

    lines = [
        "# DinoGo Database Schema", "",
        "> Actual SQL Server database schema snapshot.", ">",
        f"> Source: `{bacpac.name}` → `model.xml`",
        f"> Database: `{database}`",
        f"> Generated: {date.today().isoformat()}", ">",
        "> 此文件反映產生當下的實際 SQL Server Schema。若 Java Entity、舊 ERD、舊 Markdown 或其他文件與本文件衝突，請先回報 schema mismatch，不得自行推測或修改任一方。", "",
        "## Schema Summary", "", "| Schema | Tables |", "| --- | ---: |",
    ]
    for schema in schemas:
        count = sum(table_name(table.get("Name", "")).split(".")[0] == schema for table in tables)
        lines.append(f"| `{schema}` | {count} |")
    lines.extend((f"| **Total** | **{len(tables)}** |", "", "## Table List", "", "| Schema | Table |", "| --- | --- |"))
    for table in tables:
        schema, name = table_name(table.get("Name", "")).split(".")
        lines.append(f"| `{schema}` | `{name}` |")
    lines.append("")

    for table in tables:
        full_name = table_name(table.get("Name", ""))
        columns = child_elements(table, "Columns")
        table_constraints = [node for node in constraints if table.get("Name", "") in relationship_refs(node, "DefiningTable") or table.get("Name", "") in relationship_refs(node, "IndexedObject")]
        defaults = [node for node in table_constraints if node.get("Type") == "SqlDefaultConstraint"]

        lines.extend(("---", "", f"## `{full_name}`", "", "### Columns", "", "| Column | SQL Type | Nullable | Identity | Default | Computed |", "| --- | --- | ---: | ---: | --- | ---: |"))
        for column in columns:
            name = column_name(column.get("Name", ""))
            default_values = [markdown(property_value(node, "DefaultExpressionScript")) for node in defaults if column.get("Name", "") in relationship_refs(node, "ForColumn")]
            nullable = "NO" if property_value(column, "IsNullable") == "False" else "YES"
            identity = "YES" if property_value(column, "IsIdentity") == "True" else "NO"
            computed = "YES" if column.get("Type") == "SqlComputedColumn" else "NO"
            lines.append(f"| `{name}` | `{type_text(column)}` | {nullable} | {identity} | {'<br>'.join(default_values)} | {computed} |")
        lines.append("")

        computed_items = [f"- `{column_name(column.get('Name', ''))}`: `{markdown(property_value(column, 'ExpressionScript'))}` (persisted: {'YES' if property_value(column, 'IsPersisted') == 'True' else 'NO'})" for column in columns if column.get("Type") == "SqlComputedColumn"]
        add_list(lines, "Computed Columns", computed_items)
        primary = [node for node in table_constraints if node.get("Type") == "SqlPrimaryKeyConstraint"]
        add_list(lines, "Primary Key", [f"- `{object_name(node.get('Name', ''))}`: {', '.join(f'`{name}`' for name in constraint_columns(node))}" for node in primary])
        foreign = [node for node in table_constraints if node.get("Type") == "SqlForeignKeyConstraint"]
        add_list(lines, "Foreign Keys", [f"- `{object_name(node.get('Name', ''))}`: {', '.join(f'`{column_name(ref)}`' for ref in relationship_refs(node, 'Columns'))} → `{table_name(relationship_refs(node, 'ForeignTable')[0])}` ({', '.join(f'`{column_name(ref)}`' for ref in relationship_refs(node, 'ForeignColumns'))})" for node in foreign])
        unique = [node for node in table_constraints if node.get("Type") == "SqlUniqueConstraint"]
        add_list(lines, "Unique Constraints", [f"- `{object_name(node.get('Name', ''))}`: {', '.join(f'`{name}`' for name in constraint_columns(node))}" for node in unique])
        checks = [node for node in table_constraints if node.get("Type") == "SqlCheckConstraint"]
        add_list(lines, "Check Constraints", [f"- `{object_name(node.get('Name', ''))}`: `{markdown(property_value(node, 'CheckExpressionScript'))}`" for node in checks])
        add_list(lines, "Default Constraints", [f"- `{object_name(node.get('Name', ''))}` on `{column_name(relationship_refs(node, 'ForColumn')[0])}`: `{markdown(property_value(node, 'DefaultExpressionScript'))}`" for node in defaults])

        indexes = [node for node in table_constraints if node.get("Type") == "SqlIndex"]
        lines.extend(("### Indexes", ""))
        if not indexes:
            lines.extend(("無。", ""))
        else:
            lines.extend(("| Name | Unique | Clustered | Key columns | Included columns | Filter |", "| --- | ---: | ---: | --- | --- | --- |"))
            for index in indexes:
                specs = child_elements(index, "ColumnSpecifications")
                key_columns, included = [], []
                for spec in specs:
                    refs = relationship_refs(spec, "Column")
                    if not refs:
                        continue
                    name = column_name(refs[0])
                    if property_value(spec, "IsIncluded") == "True":
                        included.append(f"`{name}`")
                    else:
                        key_columns.append(f"`{name}` {'DESC' if property_value(spec, 'IsDescending') == 'True' else 'ASC'}")
                lines.append(f"| `{object_name(index.get('Name', ''))}` | {'YES' if property_value(index, 'IsUnique') == 'True' else 'NO'} | {'YES' if property_value(index, 'IsClustered') == 'True' else 'NO'} | {', '.join(key_columns)} | {', '.join(included)} | {markdown(property_value(index, 'FilterPredicate'))} |")
            lines.append("")

    output.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("bacpac", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    generate(args.bacpac, args.output)
