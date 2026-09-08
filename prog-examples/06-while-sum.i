routine sumTo(n: integer) is
    var total is 0
    var i is 1
    while i <= n loop
        total := total + i
        i := i + 1
    end
    print total
end
