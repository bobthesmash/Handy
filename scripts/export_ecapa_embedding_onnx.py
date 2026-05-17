#!/usr/bin/env python3
"""Export SpeechBrain ECAPA embedding to ONNX (mel input [batch, time, 80])."""

from __future__ import annotations

import argparse
from pathlib import Path

import torch
import torch.nn as nn
import torch.onnx  # noqa: F401 — import before SpeechBrain to avoid k2 lazy-import during export


class EcapaMelEmbeddingOnnx(nn.Module):
    """Wraps ECAPA embedding_model for fixed log-mel input (Handy Kotlin preprocessor)."""

    def __init__(self, embedding_model: nn.Module) -> None:
        super().__init__()
        self.embedding_model = embedding_model

    def forward(self, feats: torch.Tensor) -> torch.Tensor:
        lengths = torch.tensor([feats.shape[1]], device=feats.device)
        emb = self.embedding_model(feats, lengths)
        if emb.dim() == 3:
            emb = emb.squeeze(1)
        return emb


def main() -> None:
    import os

    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--out",
        type=Path,
        required=True,
        help="Output path, e.g. feature/voiceid/src/main/assets/voiceid/ecapa_embedding.onnx",
    )
    parser.add_argument(
        "--savedir",
        type=Path,
        default=Path("build/speechbrain-spkrec-ecapa-voxceleb"),
        help="SpeechBrain download cache (outside git assets).",
    )
    args = parser.parse_args()

    from speechbrain.inference.speaker import EncoderClassifier
    from speechbrain.utils.fetching import LocalStrategy

    hf_home = args.savedir.parent / "hf-cache"
    hf_home.mkdir(parents=True, exist_ok=True)
    os.environ.setdefault("HF_HOME", str(hf_home))

    classifier = EncoderClassifier.from_hparams(
        source="speechbrain/spkrec-ecapa-voxceleb",
        savedir=str(args.savedir),
        local_strategy=LocalStrategy.COPY,
    )
    wrapper = EcapaMelEmbeddingOnnx(classifier.mods.embedding_model)
    wrapper.eval()

    dummy = torch.randn(1, 120, 80)
    args.out.parent.mkdir(parents=True, exist_ok=True)

    torch.onnx.export(
        wrapper,
        dummy,
        str(args.out),
        input_names=["feats"],
        output_names=["embedding"],
        dynamic_axes={
            "feats": {0: "batch", 1: "time"},
            "embedding": {0: "batch"},
        },
        opset_version=17,
        dynamo=False,
    )
    print(f"Wrote {args.out} ({args.out.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
