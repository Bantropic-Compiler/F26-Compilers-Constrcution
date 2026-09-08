# Sample programs for language I

These are source programs for the Bantropic compiler project, not Java or Jasmin code. `.i` is our local convention for I source files; the supplied specification does not mandate a file extension. Each file is an independent program.

## Examples and expected results

| File | Entry routine | Arguments | Expected output values, in order | Purpose |
| --- | --- | --- | --- | --- |
| `01-print-number.i` | `show` | `42` | `42` | Print a number |
| `02-declare-variable.i` | `declare` | `10` | `10` | Declare and print a variable without reassignment |
| `03-assign-variable.i` | `change` | `10` | `20` | Reassign an existing variable |
| `04-maximum.i` | `maximum` | `7, 3` | `7` | Conditional selection |
| `05-square-function.i` | `showSquare` | `6` | `36` | Call an expression-bodied function |
| `06-while-sum.i` | `sumTo` | `5` | `15` | Sum using a while loop |
| `07-type-alias.i` | `showCount` | `5` | `5` | Use a declared type name |

Output values are specified independently of whitespace: exact print formatting is still to be defined.

## Scope of this collection

The collection contains seven independent programs covering output, variable declaration, reassignment, conditionals, function calls, a while loop, and type aliases.
