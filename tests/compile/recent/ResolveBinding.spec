        1. If _env_ is not present or if _env_ is *undefined*, then
          1. Set _env_ to the running execution context's LexicalEnvironment.
        1. Assert: _env_ is an Environment Record.
        1. If the code matching the syntactic production that is being evaluated is contained in strict mode code, let _strict_ be *true*; else let _strict_ be *false*.
        1. Return ? GetIdentifierReference(_env_, _name_, _strict_).