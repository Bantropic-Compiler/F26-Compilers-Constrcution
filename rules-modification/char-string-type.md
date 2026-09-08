# Char and string types

Not covered at all in the original spec.

```
PrimitiveType
 : integer | real | boolean | char

UserType
 : ArrayType | RecordType | StringType

StringType
 : string
```

`char` is a single Unicode code point; it is a primitive (value) type.
Literal syntax: `'a'`.

`string` is a built-in reference type, effectively a read-indexable
sequence of `char` — consistent with the spec's existing rule that
user-defined types are reference types. Literal syntax: `"text"`.

Supported operators:
- `+` — concatenation (`string + string`, `string + char`)
- `=` / `/=` — equality
- `s[i]` — indexing, 1-based, matching the existing array indexing rule
- `s.size` — length, reusing the sizeless-array convention already in the
  spec

There is no mutation of individual characters in a string — a new string
value is built via concatenation, matching the reference-type/immutable-
value distinction used elsewhere in the language.

```text
routine greet(name: string) is
    var prefix is "Hello, "
    var mark: char is '!'
    print prefix + name + mark
end
```
