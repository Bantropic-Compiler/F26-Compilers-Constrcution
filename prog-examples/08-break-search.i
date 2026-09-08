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
