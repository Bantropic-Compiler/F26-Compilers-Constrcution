routine sumOdd(n: integer) is
    var total is 0
    for i in 1..n loop
        if i % 2 = 0 then
            continue
        end
        total := total + i
    end
    print total
end
