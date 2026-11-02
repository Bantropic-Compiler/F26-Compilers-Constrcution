routine f(n : integer) : integer is
    if n > 0 then
        return 1
    end
    print n
    while n > 0 loop
        if n > 1 then
            break
        else
            continue
        end
    end
    print 2
    while n > 2 loop
        return 3
    end
    return 4
end
