# Return statement

The grammar's `Statement` production never defines `return`, even though the
prose requires one: "the value to be returned by the routine should be
specified in the routine body by the special return statement." This is a
gap in the supplied grammar, not a stylistic choice — we close it.

```
Statement
 : ...
 | ReturnStatement

ReturnStatement
 : return [ Expression ]
```

`return Expression` is used in a routine that declares a return type (a
function). Bare `return` is used in a routine with no return type (a
procedure) to exit early.

Using `return Expression` in a procedure, or bare `return` in a function, is
a compile-time error.

```text
routine factorial(n: integer): integer is
    var result is 1
    var i is 2
    while i <= n loop
        result := result * i
        i := i + 1
    end
    return result
end
```
