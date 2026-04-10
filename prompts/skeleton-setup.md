# Skeleton Setup Prompt

## AI Model Used

`GPT-5.4` via Cursor

## Prompt Text

```text
Create the skeleton for a Java 17 Gradle connector project that uses the DummyJSON posts endpoint as a stand-in for a legacy CMS API. Set up a clean, modular package structure with config, client, crawl, model, output, transform, util, and error packages. Include a runnable main entry point, environment-based configuration, source and document models, a crawler flow, JSONL output support, checkpointing scaffolding, retry and rate limiting utilities, and a basic test structure. Keep the implementation simple, production-oriented, and easy to extend as more connector behavior is added.
```

## Why I Used This Prompt

I used this prompt to get the initial project scaffold in place quickly. The goal was to start with a clean Java/Gradle connector structure and the main building blocks needed for the rest of the implementation.

## Did the Outcome Match My Intent?

Mostly yes. It was useful for getting the skeleton and overall package layout up and running, but some parts still needed follow-up refinement as the connector behavior became more concrete.
