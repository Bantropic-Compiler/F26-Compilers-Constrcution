# Numeric representation

- `integer`: signed 32-bit integer, from -2,147,483,648 to 2,147,483,647; JVM/Java `int` representation.
- `real`: IEEE 754 binary64 floating-point value; JVM/Java `double` representation. Finite magnitudes extend to approximately 1.7976931348623157e308, with approximately 15–17 significant decimal digits. Many decimal fractions are approximate.

The language keywords remain `integer` and `real`. This specifies their representation, as requested by the source language description; it does not introduce new source-level types.

The original assignment conversion rules remain in force, including rounding real values to the nearest integer and allowing integer-to-Boolean conversion only for 0 and 1. Choosing JVM representations does not automatically adopt Java cast semantics.

Still to specify separately: integer arithmetic overflow, out-of-range literals and conversions, rounding ties, and handling NaN/infinities. No decisions on these behaviors have been made here.
