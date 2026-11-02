routine f() : integer is
    return 0
end
routine t(n : integer) is
    while n > 0 loop
        break
    end
    print 3
    for i in 1..3 loop
        continue
    end
    print 5
    return
end
