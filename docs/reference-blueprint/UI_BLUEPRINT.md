# UI Blueprint — Clean-Room Reconstruction Reference

Derived from structural analysis of an open layout decompilation of a reference
motion-graphics editor APK. This document records **layout geometry, hierarchy,
and design tokens only** — no proprietary code, no copyrighted assets, no brand
strings from the original app. All Compose implementations in this repository
must follow this blueprint exactly so the interface geometry matches the
reference app's structure.

## Design Tokens (res/values/colors.xml equivalents)

### Dark neutral ramp (backdrop surfaces)
| Token | Hex | Usage in edit screen |
|---|---|---|
| A1 | #080808 | deepest overlay |
| A9 | #151515 | element inspector panel background |
| A10 | #1c1c1c | main edit screen background |
| A11 | #0c0c0c | pressed-state of panel buttons |
| S8 | #282828 | panel button normal state |
| S9 | #1d1d1d | timeline lane / value field background |
| S10 | #262626 | move&transform panel background |

### Accent
| Token | Hex | Usage |
|---|---|---|
| Y1 | #00ffa8 | primary mint-green accent (select bar, toast, playhead) |
| amAccentColor | #00ffa8 | same family |
| amAccentGradMain | #69fc97 | gradient start on timeline elements |
| amAccentGradTint | #5ce5ff | gradient end on timeline elements |

### Timeline element gradient colors (rows 1–6)
| Row | Start | End |
|---|---|---|
| 1 | #69fc97 | #5ce5ff |
| 2 | #5febff | #3c8ce7 |
| 3 | #7466f0 | #ce9ffc |
| 4 | #f16586 | #fad3a5 |
| 5 | #e6d68c | #e6f597 |
| 6 | #52eee6 | #4ea3d5 |

### Text
| Token | Hex | Usage |
|---|---|---|
| W1 | #ffffff | primary text |
| W1_a30 | 30% white | secondary labels (blend mode names) |
| G1 | #eaeaec | tertiary text |

## Edit Screen Layout Hierarchy (activity_edit.xml)

```
ConstraintLayout (bg A10)
├── navBar (42dp) — include activity_edit_navbar
├── previewGuide (SurfaceView, bg A11)
├── viewmodeHolder (50dp wide, bg S9, right edge)
│   ├── buttonPixelMode
│   ├── buttonGridMode
│   ├── buttonSoloMode
│   ├── buttonCameraMode
│   └── canvasZoomButtonHolder
├── timeline (RecyclerView, 280dp)
├── playBar (46dp) — include activity_edit_playbar
├── infoBarHolder (46dp) — include activity_edit_infobar
├── timelineOverflowButton (40×40dp)
├── buttonAdd (73×73dp, FAB)
├── addPopup (251dp) — include activity_edit_addpopup
├── elementFragmentHolder (200dp) — fragment_element_edit
├── selectBottomOptionsBar (42dp)
└── fullScreenPlayer
```

### Key dimensions
| Name | Value |
|---|---|
| edit_timeline_height | 280dp |
| edit_panel_height | 200dp |
| add_button_size | 73dp |
| add_button_radius | 28dp |
| add_popup_height | 251dp |
| add_popup_bottom_panel_height | 125dp |
| add_popup_tab_indicator_size | 5.5dp |
| timeline_row_height | 28dp |
| timeline_tick_width | 1dp |
| timeline_tick_sec | 18dp |
| timeline_tick_halfsec | 12dp |
| timeline_base_dp_per_sec | 100dp |
| timeline_scroll_per_second | 120dp |
| timeline_grip_width | 35dp |
| timeline_element_trim_grip_size | 30dp |
| timeline_element_corner_rad | 1.5dp |
| timeline_header_width_expanded | 70dp |
| timeline_top_space | 42dp |
| timeline_cts_text_size | 14dp |

## NavBar (activity_edit_navbar.xml) — 42dp tall

Horizontal LinearLayout:
1. `exitToProjectList` — 40×42dp ImageButton (back arrow)
2. `projectTitle` — SettingEditText (editable project name)
3. `editNestedNaviRecyclerView` — breadcrumb nav
4. `editTimeView` — current timecode
5. `projectSettings` — 40×42dp ImageButton (gear)
6. `share` — 40×42dp ImageButton (export/share)
7. `groupSettings` — 40×42dp ImageButton (visible when group selected)

## Playbar (activity_edit_playbar.xml) — 46dp tall

ConstraintLayout containing:
- `levelMeterView` — 2dp tall LevelMeterView strip (audio VU meter, sits at top of playbar)
- `buttonUndo` / `buttonRedo`
- `buttonSkipBack` (prev frame) / `buttonPlay` / `buttonSkipFwd` (next frame)
- `buttonCopyPasteOverflowContainer` with CardView "NEW" badge
- `buttonBookmark` (bg playbar_toggle_button_bg: 4.5dp inset rounded rect, activated = 5% white, pressed = 6.3% white)
- `buttonPreviewMode`

## InfoBar (activity_edit_infobar.xml) — 46dp tall

Two modes:
- **iconMode:** `infoModeIcon` + `infoProjectTime` + `infoOffsetIcon` + `infoOffsetTime`
- **textMode:** six label/value pairs in columns (infoBar1L/infoBar1 … infoBar3L/infoBar6)

## Select Bar (activity_edit_selectbar.xml) — 42dp tall, bg Y1

- `selectStateMessage`
- `buttonGroupSelected` / `buttonGroupAndMask` / `buttonGroupAndExclude` / `buttonDeleteSelected` / `buttonRightCue`
- align screen buttons: left/centerH/right/top/centerV/bottom/distributeV/distributeH
- `cancelSelectMode` (bg Y1)

## Select Bottom Bar (activity_edit_select_bottomvar.xml) — 42dp tall, bg rounded_bg_s9_5dp

- `buttonMultiTrimLeft` / `buttonMultiSplit` / `buttonMultiTrimRight`
- `buttomMultiExpandEnd` / `buttomMultiMoveToRight` / `buttomMultiExpandStart` / `buttomMultiMoveToLeft`
- `alignLeftTimeline` / `alignDistributeTimeline` / `alignRightTimeline`

## Element Inspector (fragment_element_edit.xml) — 200dp tall, bg A9

Row structure:
1. **Speed row:** `buttonSpeedControl` (speed icon + arrow, NEW badge), grouped pill `buttonTrimLeft`/`buttonSplit`/`buttonTrimRight` (bg: left=4dp-left-corner-radius S8, center=flat S8, right=4dp-right-corner-radius S8; pressed all → A11), `buttonMoveOrExpandLeft/Right`, `buttonAudio` (+ cue badge)
2. **Style row:** AppCompatButtons `buttonColorAndFill`, `buttonBorderAndShadow`, `buttonBlendingAndOpacity`
3. **Transform row:** `buttonMoveAndTransform` (icon+label), `buttonEditPoints`, `buttonEditText`, `buttonEditComp`, `buttonEditDrawing`, `buttonEditCamera`, `buttonEditLiveShape`, `buttonPresets` (Lottie + "Presets" label, NEW badge), `buttonEffects`

Speed control panel (inline expandable, bg A9, inner round_rect4dp_nor_s8_pre_a11):
- `retimeExtendStart` / `retimeReduceStart` / `retimeReduceEnd` / `retimeExtendEnd` (bg: S9 rounded 4dp corners, pressed → Bl1_a10)
- `iconSlow` / `iconFast` / `speedControlSetting` (AlightSlider) / `speedControlText` (bg round_rect4dp_w1_a5)

## Move & Transform (fragment_move_transform.xml) — bg S10

- `trackpad` (ControlPad) + `trackpadGuideText` "Swipe here to move layer"
- `pivotPad` (ControlPad) + `pivotGuideText` "Swipe here to move pivot point"
- `rotatePad` (RotateView)
- Z-position guide: `trackpadZGuidUp` + "Swipe here to adjust Z position" + `trackpadZGuidDown`
- Corner markers: `corner_ne/nw/se/sw`
- `locationX` (bg left_round_rect5dp_s9) + `locationY` (bg right_round_rect5dp_s9) + labels
- `locationZ` (bg round_rect5dp_s9)
- `elementWidth`/`elementHeight` (bg round_rect5dp_s9) + Width/Height labels + `elementLink` (bg panel_lightbutton_bg)
- `scaleSpinner`/`scaleSpinnerWidth`/`scaleSpinnerHeight` (ValueSpinner)
- `skewAmountX`/`skewAmountY` (bg round_rect5dp_s9) + X SKEW / Y SKEW labels + `skewSpinnerX`/`skewSpinnerY`

## Color & Fill (fragment_color_and_fill.xml) — bg A9

- `tab_holder` (CardView): `btn_none` / `btn_fill` / `btn_gradation` / `btn_media` / `btn_intrinsic` (bg selectable_tab_menu_bg: activated → A3, disabled → S8_a30, normal → S9)
- `none_view` (hidden layer fill preview)
- `color_picker` (ColorPickerWidget)
- **Gradation holder:** 3 type buttons (gradation_type1/2/3, bg selector_nor_s9_pre_s10), `gradation_bar` (GradationBar) + `color_start`/`color_end` (ColorView)
- **Media holder:** `selected_media` (bg round_rect_bg_s10_5dp) with "Select media" label, `mediaCropButtons`: Fill / Fit / Stretch (bg selector_nor_s9_pre_s10)

## Blending & Opacity (fragment_blending_opacity.xml) — bg A9

- `blendModes` (RecyclerView of blend_mode.xml cells: 60×80dp, 45dp icon + 11dp label in W1_a30)
- `opacitySlider` (SeekBar)
- `opacityValue` (bg round_rect4dp_bl1_20 — 12% black pill)

**Full blend mode list (35 modes in 7 categories):**
- Normal: Normal, Mask
- Darken: Darken, Multiply, Color Burn, Linear Burn, Darker Color
- Lighten: Lighten, Screen, Color Dodge, Linear Dodge, Lighter Color
- Contrast: Overlay, Soft Light, Hard Light, Vivid Light, Linear Light, Pin Light, Soft Overlay
- Difference: Difference, Exclusion, Divide, Subtract
- Color: Hue, Saturation, Color, Luminance
- (Mask category = Mask)

## Border & Shadow (fragment_border_and_shadow.xml) — bg A9

- `color_switch_holder`: ColorView + feature title + SwitchCompat
- `strokeWidthSpinner` (ValueSpinner) + `strokeWidthValue` (bg round_rect4dp_bl1_20)
- `capSelector` (CardView): Butt / Round / Square (bg selector_nor_s10_pre_s9)
- `joinSelector` (CardView): Bevel / Round / Miter
- `startShapeStyle` ("Start" label + shape icon) / `endShapeStyle` ("End" label + shape icon)
- `endStyleSettingHolder` (bg top_round_no_blur_menu_bg): `endSizeSpinner` + shape RecyclerView
- `slider_border` + `border_value`
- `shadowSettingsList` + `borderList` (RecyclerViews)

## Add Popup (activity_edit_addpopup.xml) — 251dp tall, bg add_menu_bg

**Left elementTabHolder (tabs):**
1. `tabAddShape` (ic_add_object, "Shape")
2. `tabAddImageVideo` (ic_add_imagevideo, "Media")
3. `tabAddAudio` (ic_add_audio, "Audio")
4. `tabAddObject` (ic_add_object_element, "Object / Element")
5. `tabAddTemplates` (ic_add_templates, "Template", NEW badge)

**Right quickAddBar (vertical column):**
- `buttonAddFreehandDrawing` (ic_add_freehanddrawing, "Freehand Drawing")
- `buttonAddVectorDrawing` (ic_add_vectordrawing, "Vector Drawing")
- `buttonAddText` (text layer)
- `addElementsProjects` ("Element / Project")

**Tab content:**
- Shape tab: grid of `add_shape_listentry` cells (shapeThumbnail only, no labels)
- Media tab: `add_media_visualmedia_item` grid (CardView + thumbnail + duration badge)
- Audio tab: `addAudioList` (RecyclerView)
- Object tab: `addCameraObject` (Camera, members badge), `addNullObject` ("Null"), `addEmptyGroup` ("Empty Group")
- Templates tab: `addCompList` (RecyclerView)

**Add popup visuals:** panel bg #20282828 (S8 @ 12.5%), side tab bg #4c262626, selected side tab bg #b2212121, tab indicator 5.5dp, corner radius 13.5dp

**Shape list (19 shapes):** Rectangle, Rounded Rectangle, Circle, Triangle, Quad, Polygon, Star, Multifoil, Penta (Irregular Pentagon), Moon, Arc, Pie, Plus, Teardrop, Stamp, Arrow, Callout, Line, Wide Line

## Timeline Item (timeline_item.xml)

CardView (`elementFrame`) containing:
- `TimelineBackgroundView` (gradient body — amElement N Grad colors)
- `elementThumbnails` (ThumbnailView strip)
- `audioThumbnails` (AudioThumbnailView waveform)
- `clippingMaskIcon` + `elementLabel`
- `elementKeyframes` (KeyframeView diamond markers)
- `trimGripLeft` / `trimGripRight` (35dp wide grip zones, 30dp trim grip)

## Timeline Header (timeline_header.xml)

- `clippingMaskIndicator`
- `headerBg`
- `trackThumb` (layer thumbnail)
- `trackTag`
- `layerVisibilityToggle` (eye)
- `layerSelectionCheckmark`

## Element Actionbar (fragment_element_actionbar.xml) — replaces navbar when layer selected

LinearLayout: `navBack` → `elementLabel` (SettingEditText) → `layerParent` (icon + members badge) → `delete` → `overflow`

## Effect Panel (fragment_effects.xml)

- `effectList` (RecyclerView of effect_list_item rows: icon + effectName + premium lock + visibility + grip)
- `effectSettingsHolder`: header (effect icon, settingsEffectName, buttonFxOverflow, buttonDelete) + `effectSettingsList` (parameter rows)

## Edit Points (fragment_edit_points2.xml)

- `outlineScroller` (OutlineScrollerView)
- `backButton` / `openCloseOutlineButton` / `buttonCurveType` / `buttonAddRemoveCtrlPoint` / `addRemoveKeyframeButton` / `editEasingButton` / `keyingOverflow`
- `movePointTrackpad` + corner markers + guide text
- `contourInfo` + `prevContour` / `nextContour`

## Copy/Paste Overflow Menu (layer_copy_paste_menu_popup.xml)

RecyclerView of copy actions:
Copy Layer Style, Copy Transform, Copy Rotation, Copy Scale, Copy Skew, Copy Pivot, Copy Location, Copy Curve, Copy Color Code, Copy Effect(s), Copy Free Effects, Copy All Effects

Paste options panel:
- pasteTitle "Paste Style"
- Row 1: ColorAndFill / BorderAndShadow / BlendingAndOpacity buttons
- Row 2: MoveAndTransform / EditText / Volume / Effects / Speed buttons
- `pasteButton` "Paste" + `cancelButton` "Cancel"

## String Registry (button labels as they appear)

- Speed / Trim / Split (trim & split row)
- Color & Fill / Border & Shadow / Blending & Opacity
- Move & Transform / Edit Points / Edit Text / Edit Comp / Edit Drawing / Camera Options / Edit Shape / Presets / Effects
- Shape / Media / Audio / Object / Element / Template (add tabs)
- Freehand Drawing / Vector Drawing / Edit Text / Camera / Null / Empty Group / Element / Project
- Swipe here to move layer / Swipe here to move pivot point / Swipe here to adjust Z position
- Width / Height / X SKEW / Y SKEW / Start / End
- Select media / Fill / Fit / Stretch
- Copy / Paste / Copy Layer Style / Copy Transform / Copy Rotation / Copy Scale / Copy Skew / Copy Pivot / Copy Location / Copy Curve / Copy Color Code / Copy Effect
- Allow Access / This project uses external video, photo or audio media...
- Swipe to Adjust

## Interaction Map (buttons → menus, no features)

| Button tap | Opens |
|---|---|
| buttonAdd (FAB) | addPopup (251dp bottom panel) |
| tabAddShape/Media/Audio/Object/Templates | tab content switch |
| buttonColorAndFill | Color & Fill panel (200dp) |
| buttonBorderAndShadow | Border & Shadow panel (200dp) |
| buttonBlendingAndOpacity | Blending & Opacity panel (200dp) |
| buttonMoveAndTransform | Move & Transform panel (200dp) |
| buttonEditPoints | Edit Points panel |
| buttonEditText | Text edit screen |
| buttonEffects | Effects panel |
| buttonSpeedControl | inline Speed panel (expandable) |
| buttonAudio | Volume panel |
| buttonCopyPasteOverflow | Copy/Paste menu popup |
| projectSettings | Project Settings dialog |
| share | Export screen |
| timelineOverflowButton | Timeline options menu |
| buttonPreviewMode | Full-screen preview |
| select bar buttons | multi-select actions |
