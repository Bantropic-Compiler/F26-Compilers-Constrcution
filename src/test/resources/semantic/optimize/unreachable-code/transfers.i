routine f() : integer is
    return 0
    print 1
    var unused is 2
end
routine t(n : integer) is
    while n > 0 loop
        break
        print 2
    end
    print 3
    for i in 1..3 loop
        continue
        print 4
    end
    print 5
    return
    print 6
end
