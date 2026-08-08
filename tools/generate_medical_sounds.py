"""Build the mod's medical Foley from real field recordings.

The source recordings and their licensing details live in art_sources/audio and
art_sources/audio/SOURCES.md.  This script only trims, layers, resamples and
normalises those recordings; it does not synthesise procedural noise.
"""

from pathlib import Path

import numpy as np
import soundfile as sf


SAMPLE_RATE = 48_000
ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "art_sources" / "audio"
OUTPUT_DIR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "wok_trauma"
    / "sounds"
)


def resample(samples: np.ndarray, source_rate: int) -> np.ndarray:
    if source_rate == SAMPLE_RATE:
        return samples.astype(np.float64, copy=False)
    output_size = round(samples.size * SAMPLE_RATE / source_rate)
    source_positions = np.arange(samples.size, dtype=np.float64)
    output_positions = np.linspace(0.0, samples.size - 1.0, output_size)
    return np.interp(output_positions, source_positions, samples)


def read_mono(filename: str) -> np.ndarray:
    samples, source_rate = sf.read(SOURCE_DIR / filename, always_2d=True)
    mono = samples.astype(np.float64).mean(axis=1)
    return resample(mono, source_rate)


def clip(samples: np.ndarray, start: float, end: float) -> np.ndarray:
    first = max(0, round(start * SAMPLE_RATE))
    last = min(samples.size, round(end * SAMPLE_RATE))
    return samples[first:last].copy()


def moving_average(samples: np.ndarray, width: int) -> np.ndarray:
    width = max(1, width)
    padded = np.pad(samples, (width, 0), mode="edge")
    cumulative = np.cumsum(padded, dtype=np.float64)
    return (cumulative[width:] - cumulative[:-width]) / width


def fade(samples: np.ndarray, fade_in: float = 0.012, fade_out: float = 0.025) -> np.ndarray:
    result = samples.copy()
    in_size = min(result.size, round(fade_in * SAMPLE_RATE))
    out_size = min(result.size, round(fade_out * SAMPLE_RATE))
    if in_size:
        result[:in_size] *= np.linspace(0.0, 1.0, in_size)
    if out_size:
        result[-out_size:] *= np.linspace(1.0, 0.0, out_size)
    return result


def peak_normalize(samples: np.ndarray, peak: float) -> np.ndarray:
    samples = samples - float(np.mean(samples))
    maximum = float(np.max(np.abs(samples)))
    if maximum > 0.0:
        samples *= peak / maximum
    return samples.astype(np.float32)


def mix_into(target: np.ndarray, source: np.ndarray, start: float, gain: float) -> None:
    first = round(start * SAMPLE_RATE)
    available = min(source.size, target.size - first)
    if available > 0:
        target[first : first + available] += source[:available] * gain


def injector_puncture() -> np.ndarray:
    """A dry puncture followed by a close syringe/plunger movement."""
    needle_source = read_mono("needle_fabric_wavjunction_456772.mp3")
    syringe_source = read_mono("syringe_taure_465495.mp3")

    needle = fade(clip(needle_source, 1.30, 1.76), 0.008, 0.035)
    # A low-passed layer gives the recorded fabric puncture a restrained,
    # body-conducted thump without adding synthetic sound.
    needle_body = moving_average(needle, 31)
    mechanism = fade(clip(syringe_source, 7.76, 8.23), 0.008, 0.030)

    result = np.zeros(round(0.72 * SAMPLE_RATE), dtype=np.float64)
    mix_into(result, needle, 0.025, 0.62)
    mix_into(result, needle_body, 0.025, 0.42)
    mix_into(result, mechanism, 0.205, 0.30)
    return peak_normalize(fade(result), 0.68)


def canvas_bag_open() -> np.ndarray:
    """One close, medium-fast pass of a real rigid canvas-bag zipper."""
    source = read_mono("canvas_zipper_vintage2005_438985.mp3")
    zipper_pass = fade(clip(source, 5.22, 6.24), 0.018, 0.055)
    return peak_normalize(zipper_pass, 0.70)


def bandage_wrap() -> np.ndarray:
    """Two compact cloth pulls from a recording made to emulate bandaging."""
    source = read_mono("wrapping_cloth_chucklenutsdev_718668.mp3")
    wrap = fade(clip(source, 1.70, 2.43), 0.018, 0.055)
    # Reduce the very lowest handling rumble while retaining natural cloth body.
    wrap = wrap - moving_average(wrap, 241) * 0.72
    return peak_normalize(wrap, 0.64)


def write_sound(name: str, samples: np.ndarray) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    sf.write(
        OUTPUT_DIR / f"{name}.ogg",
        samples,
        SAMPLE_RATE,
        format="OGG",
        subtype="VORBIS",
    )


def main() -> None:
    write_sound("injector_puncture", injector_puncture())
    write_sound("canvas_bag_open", canvas_bag_open())
    write_sound("bandage_wrap", bandage_wrap())


if __name__ == "__main__":
    main()
