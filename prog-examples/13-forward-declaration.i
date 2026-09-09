routine isOdd(n: integer): boolean

routine isEven(n: integer): boolean is
    if n = 0 then
        return true
    end
    return isOdd(n - 1)
end

routine isOdd(n: integer): boolean is
    if n = 0 then
        return false
    end
    return isEven(n - 1)
end

routine checkEven(n: integer) is
    print isEven(n)
end
