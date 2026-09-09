routine factorial(n: integer): integer is
    var result is 1
    var i is 2
    while i <= n loop
        result := result * i
        i := i + 1
    end
    return result
end

routine showFactorial(n: integer) is
    print factorial(n)
end
