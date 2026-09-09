routine fibonacci(n: integer): integer is
    if n <= 1 then
        return n
    end
    return fibonacci(n - 1) + fibonacci(n - 2)
end

routine showFibonacci(n: integer) is
    print fibonacci(n)
end
