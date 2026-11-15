"""Build an Android asset directory from a local, read-only champion-guides snapshot."""

import argparse
from datetime import date
import json
from pathlib import Path
import re
import shutil
from tempfile import TemporaryDirectory


def identifier(value):
    if not isinstance(value, str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]*", value):
        raise ValueError(f"Invalid catalogue identifier: {value!r}")
    return value


def distinct_ids(entries, kind):
    ids = {entry["id"] for entry in entries}
    if len(entries) != len(ids):
        raise ValueError(f"Duplicate {kind} IDs")
    return ids


def build_record(champion_id, build, relic_ids):
    slots = [slot["options"] for slot in build["slots"]]
    if not 1 <= len(slots) <= 3 or any(
        not options or len(set(options)) != len(options) or not set(options) <= relic_ids
        for options in slots
    ):
        raise ValueError(f"Invalid relic slots for {champion_id}/{build['id']}")
    return {
        "id": f"{champion_id}/{identifier(build['id'])}",
        "title": build["title_en"],
        "stars": build.get("stars"),
        "maxStars": build.get("max_stars"),
        "level": build.get("level"),
        "slots": slots,
        "strategy": build.get("strategy_en") or None,
        "notes": build.get("notes_ru") or None,
        "source": build["url"],
        "requirementsSource": build.get("requirements_url"),
    }


def champion_record(source, destination, champion, relic_ids):
    champion_id = identifier(champion["slug"])
    original_portrait = source / "assets" / "champions" / f"{champion_id}.webp"
    if champion["portrait_file"] != original_portrait.relative_to(source).as_posix():
        raise ValueError(f"Unexpected portrait path for {champion_id}")
    if original_portrait.resolve(strict=True).parent != (source / "assets" / "champions").resolve():
        raise ValueError(f"Portrait resolves outside its directory: {champion_id}")
    portrait = f"portraits/{champion_id}.webp"
    with original_portrait.open("rb") as image:
        header = image.read(12)
    if header[:4] != b"RIFF" or header[8:] != b"WEBP":
        raise ValueError(f"Expected a WebP portrait for {champion_id}")
    shutil.copyfile(original_portrait, destination / portrait)

    builds = [build_record(champion_id, build, relic_ids) for build in champion["builds"]]
    distinct_ids(builds, f"build for {champion_id}")
    return {
        "id": champion_id,
        "name": champion["name_ru"],
        "originalName": champion["name_en"],
        "aliases": champion["aliases"],
        "plan": champion["plan_ru"],
        "mulligan": champion["mulligan_ru"],
        "caution": champion["caution_ru"],
        "portrait": portrait,
        "builds": builds,
        "starPowers": [
            {"star": int(power["star"]), "name": power["name"], "description": power["description"]}
            for power in champion["star_powers_ru"]
        ],
        "referenceGuides": [
            {"title": guide["title_en"], "url": guide["url"]}
            for guide in champion["reference_builds"]
        ],
    }


def export_catalog(source, output):
    source = source.resolve(strict=True)
    output = output.resolve()
    if output.exists():
        raise FileExistsError(f"Choose a new export directory: {output}")
    data = json.loads((source / "database.json").read_text(encoding="utf-8"))
    if data["schema"] != 1 or data["champion_count"] != len(data["champions"]):
        raise ValueError("Unsupported or incomplete champion-guides snapshot")
    relics = [
        {
            "id": identifier(relic["code"]),
            "name": relic["name_ru"],
            "description": relic["description_ru"],
        }
        for relic in data["relics"]
    ]
    relic_ids = distinct_ids(relics, "relic")
    output.parent.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(prefix=".catalog-export-", dir=output.parent) as scratch:
        destination = Path(scratch) / "assets"
        (destination / "portraits").mkdir(parents=True)
        champions = [
            champion_record(source, destination, champion, relic_ids)
            for champion in data["champions"]
        ]
        distinct_ids(champions, "champion")
        catalog = {
            "schemaVersion": 1,
            "snapshot": {
                "patch": data["patch"],
                "checkedDate": date.fromisoformat(data["checked_date"]).isoformat(),
            },
            "relics": relics,
            "champions": champions,
        }
        (destination / "catalog.json").write_text(
            json.dumps(catalog, ensure_ascii=False, separators=(",", ":")) + "\n",
            encoding="utf-8",
        )
        destination.rename(output)
    print(f"{len(champions)} champions, {len(relics)} relics, "
          f"{sum(len(champion['builds']) for champion in champions)} builds")
    print(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="Root of the existing champion-guides library")
    parser.add_argument("output", type=Path, help="New local directory for Android assets")
    args = parser.parse_args()
    export_catalog(args.source, args.output)
