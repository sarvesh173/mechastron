#!/usr/bin/env python3
"""Mechastron Agent Bridge — Autonomous Motion Graphics Generation Tool.

Enables AI agents to author Mechastron project XMLs and .mepack archives
directly without requiring server-side heavy video rendering.
"""

import json
import os
import shutil
import sys
import xml.etree.ElementTree as ET
import zipfile


class MechastronProjectBuilder:

    def __init__(
        self,
        title: str,
        width: int = 1080,
        height: int = 1920,
        fps: int = 60,
        duration_ms: int = 5000,
    ):
        self.title = title
        self.width = width
        self.height = height
        self.fps = fps
        self.duration_ms = duration_ms
        self.layers = []

    def add_layer(
        self,
        layer_id: str,
        name: str,
        layer_type: str = "image",
        asset_ref: str | None = None,
        parent_id: str | None = None,
        start_ms: int = 0,
        end_ms: int | None = None,
    ):
        layer = {
            "id": layer_id,
            "name": name,
            "type": layer_type,
            "asset_ref": asset_ref,
            "parent": parent_id,
            "start_time": start_ms,
            "end_time": end_ms or self.duration_ms,
            "transform": {
                "pos_x": {"default": 0.0, "keyframes": []},
                "pos_y": {"default": 0.0, "keyframes": []},
                "scale_x": {"default": 1.0, "keyframes": []},
                "scale_y": {"default": 1.0, "keyframes": []},
                "rotation_deg": {"default": 0.0, "keyframes": []},
                "opacity": {"default": 1.0, "keyframes": []},
            },
            "effects": [],
        }
        self.layers.append(layer)
        return layer

    def add_keyframe(
        self,
        layer: dict,
        prop: str,
        time_ms: int,
        value: float,
        ease: str = "0.4,0.0,0.2,1.0",
    ):
        if prop in layer["transform"]:
            layer["transform"][prop]["keyframes"].append(
                {"time": time_ms, "value": value, "ease": ease}
            )

    def to_xml(self) -> str:
        root = ET.Element(
            "mechastron_project",
            {
                "title": self.title,
                "width": str(self.width),
                "height": str(self.height),
                "fps": str(self.fps),
                "duration_ms": str(self.duration_ms),
                "engine": "Mechastron v1.0",
            },
        )

        for l_data in self.layers:
            attrs = {
                "id": l_data["id"],
                "name": l_data["name"],
                "type": l_data["type"],
                "start_time": str(l_data["start_time"]),
                "end_time": str(l_data["end_time"]),
            }
            if l_data["parent"]:
                attrs["parent"] = l_data["parent"]
            if l_data["asset_ref"]:
                attrs["asset_ref"] = l_data["asset_ref"]

            l_elem = ET.SubElement(root, "layer", attrs)
            t_elem = ET.SubElement(l_elem, "transform")

            for prop_name, prop_data in l_data["transform"].items():
                tr_attrs = {
                    "name": prop_name,
                    "default": str(prop_data["default"]),
                }
                tr_elem = ET.SubElement(t_elem, "track", tr_attrs)
                for kf in prop_data["keyframes"]:
                    ET.SubElement(
                        tr_elem,
                        "keyframe",
                        {
                            "time": str(kf["time"]),
                            "value": str(kf["value"]),
                            "ease": kf["ease"],
                        },
                    )

        ET.indent(root, space="  ")
        return '<?xml version="1.0" encoding="UTF-8"?>\n' + ET.tostring(
            root, encoding="unicode"
        )

    def export_mepack(self, output_path: str, assets_map: dict | None = None):
        """Bundles project into an offline .mepack ZIP file."""
        os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
        xml_content = self.to_xml()

        with zipfile.ZipFile(
            output_path, "w", compression=zipfile.ZIP_DEFLATED
        ) as zf:
            zf.writestr("project.xml", xml_content)
            if assets_map:
                for target_rel_path, src_path in assets_map.items():
                    if os.path.exists(src_path):
                        zf.write(src_path, arcname=f"assets/{target_rel_path}")


if __name__ == "__main__":
    builder = MechastronProjectBuilder("Titan Cyber Launch", 1080, 1920, 60, 4000)
    l1 = builder.add_layer(
        "bg_mesh", "Background Solid", "solid_color", start_ms=0, end_ms=4000
    )
    l2 = builder.add_layer(
        "hero_mech",
        "Mechastron Hero",
        "image",
        asset_ref="assets/hero.png",
        start_ms=0,
        end_ms=4000,
    )

    # Animate scale & position with easing
    builder.add_keyframe(l2, "scale_x", 0, 0.0, "0.1,0.9,0.2,1.0")
    builder.add_keyframe(l2, "scale_x", 1200, 1.0, "0.4,0.0,0.2,1.0")
    builder.add_keyframe(l2, "pos_y", 0, 800.0, "0.175,0.885,0.32,1.275")
    builder.add_keyframe(l2, "pos_y", 1500, 960.0, "0.42,0.0,0.58,1.0")

    out_file = "/tmp/test_project.xml"
    with open(out_file, "w") as f:
        f.write(builder.to_xml())

    print(f"✓ Agent generated test project XML: {out_file}")
