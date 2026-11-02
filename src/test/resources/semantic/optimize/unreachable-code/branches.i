routine f(n : integer) : integer is
    if n > 0 then
        if n > 1 then
            return 1
            print 11
        else
            return 2
            print "something"
        end
        print 12
    else
        return 3
        print 13
        f(4)
    end
    print 14
    f(25)
end
