routine t(n : integer) is
    if true then
        var unique is n
        print unique
    else
        print 2
    end
    if false then
        print 3
    else
        print 4
    end
    if 0 then
        print 5
    end
    while false loop
        print 6
    end
    for i in 3..1 loop
        print i
    end
    for j in 3..1 reverse loop
        print j
    end
end
