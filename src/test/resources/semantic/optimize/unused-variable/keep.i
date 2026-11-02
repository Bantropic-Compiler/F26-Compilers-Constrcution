routine f() : integer is
    print 1
    return 2
end
routine t(p : integer, r : real, a : array [3] integer) is
    var called is f()
    var written is 1
    written := f()
    var division is 10 / p
    var overflow is p + 1
    var indexed is a[p]
    var converted : integer is r
    var used is p
    print used
    var counter is 0
    counter := counter + 1
    a[p] := 3
end
