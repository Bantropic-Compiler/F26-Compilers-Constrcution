# Break and continue

Not mentioned anywhere in the source description.

```
Statement
 : ...
 | BreakStatement
 | ContinueStatement

BreakStatement
 : break

ContinueStatement
 : continue
```

`break` exits the nearest enclosing `while` or `for` loop immediately.

`continue` jumps to the next iteration:
- in a `while` loop, control returns to condition re-evaluation;
- in a `for` loop, the loop variable is updated first (per the existing
  increment/decrement/reverse rules), then the range bound is checked.

Using `break` or `continue` outside a loop is a compile-time error.

```text
routine firstDivisor(n: integer) is
    var i is 2
    var result is n
    while i <= n loop
        if n % i = 0 then
            result := i
            break
        end
        i := i + 1
    end
    print result
end
```
