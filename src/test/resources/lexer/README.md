# Golden token fixtures

One fixed `.tokens` file per `prog-examples/*.i`. Each row contains four
tab-separated fields: `line:column`, token type, exact lexeme, typed value.
Backslashes, tabs, CR and LF are escaped. `-` means a null value.

The initial fixtures were built using an independent regex-based reference,
not by recording the Java lexer output. Tests only read these files; they
never regenerate expectations. Review changed expectations against the
source example when the language or an example changes.

`=>` in example 05 is `EQ` followed by `GT`, as the current token catalog
does not define an arrow token. This is lexical coverage, not syntax validation.

`CategoryTestDriver` connects category scanners exclusively in test code.
The production `Lexer.nextToken()` integration belongs to stage 4.
